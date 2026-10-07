package com.aarav.didyoudoit.viewmodel;

import com.aarav.didyoudoit.model.Priority;
import com.aarav.didyoudoit.model.StreakInfo;
import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.model.TaskCategory;
import com.aarav.didyoudoit.model.TaskStatus;
import com.aarav.didyoudoit.service.RecurringTaskEngine;
import com.aarav.didyoudoit.service.StreakService;
import com.aarav.didyoudoit.service.TaskService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * ViewModel managing UI state and asynchronous operations for the Daily Dashboard.
 * Keeps business logic decoupled from JavaFX views and never blocks the Application Thread.
 * Implements FR-01, FR-02, FR-03, FR-09, FR-12, FR-14, FR-21, NFR-02.
 */
public class DashboardViewModel {

    private static final Logger LOGGER = Logger.getLogger(DashboardViewModel.class.getName());

    public enum DashboardSection {
        TODAY("Today's Tasks"),
        OVERDUE("Overdue & Nagging"),
        HABITS("Daily Habits"),
        STATISTICS("Progress & Stats"),
        SETTINGS("Settings"),
        COMPLETED("Completed Tasks");

        private final String title;

        DashboardSection(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }

    private final TaskService taskService;
    private final RecurringTaskEngine recurringEngine;
    private final StreakService streakService;

    // Observable Lists for UI binding
    private final ObservableList<Task> displayedTasks = FXCollections.observableArrayList();
    private final ObservableList<Task> allLoadedTasks = FXCollections.observableArrayList();

    // State properties
    private final ObjectProperty<DashboardSection> activeSection = new SimpleObjectProperty<>(DashboardSection.TODAY);
    private final ObjectProperty<TaskCategory> selectedCategoryFilter = new SimpleObjectProperty<>(null);
    private final StringProperty searchQuery = new SimpleStringProperty("");

    // Metrics properties
    private final IntegerProperty activeTasksCount = new SimpleIntegerProperty(0);
    private final IntegerProperty overdueTasksCount = new SimpleIntegerProperty(0);
    private final IntegerProperty completedTasksCount = new SimpleIntegerProperty(0);
    private final IntegerProperty bestStreakCount = new SimpleIntegerProperty(0);
    private final BooleanProperty isLoading = new SimpleBooleanProperty(false);
    private final StringProperty statusMessage = new SimpleStringProperty("Ready");

    public DashboardViewModel(TaskService taskService,
                              RecurringTaskEngine recurringEngine,
                              StreakService streakService) {
        this.taskService = Objects.requireNonNull(taskService, "taskService cannot be null");
        this.recurringEngine = Objects.requireNonNull(recurringEngine, "recurringEngine cannot be null");
        this.streakService = Objects.requireNonNull(streakService, "streakService cannot be null");

        // React to filter or search query changes
        searchQuery.addListener((obs, oldVal, newVal) -> applyFilters());
        selectedCategoryFilter.addListener((obs, oldVal, newVal) -> applyFilters());
        activeSection.addListener((obs, oldVal, newVal) -> reloadTasks());
    }

    /**
     * Asynchronously loads daily tasks, spawns recurring templates for today, and updates metrics.
     */
    public CompletableFuture<Void> reloadTasks() {
        isLoading.set(true);
        return CompletableFuture.runAsync(() -> {
            try {
                // Ensure daily instances exist for today's recurring habits
                recurringEngine.generateDailyInstances(LocalDate.now());

                List<Task> tasksToLoad = switch (activeSection.get()) {
                    case TODAY -> taskService.getTodayTasks();
                    case OVERDUE -> taskService.getOverdueTasks();
                    case HABITS -> taskService.getActiveTasks().stream()
                            .filter(t -> t.getParentTemplateId() != null || t.isTemplate())
                            .toList();
                    case STATISTICS -> List.of();
                    case SETTINGS -> List.of();
                    case COMPLETED -> taskService.searchTasks(null, null, null, TaskStatus.COMPLETED);
                };

                // Compute summary metrics
                List<Task> todayActive = taskService.getTodayTasks().stream()
                        .filter(t -> t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.DELETED)
                        .toList();
                List<Task> overdue = taskService.getOverdueTasks();
                StreakService.StreakStats stats = streakService.getStreakStats();

                runOnFxThread(() -> {
                    allLoadedTasks.setAll(tasksToLoad);
                    activeTasksCount.set(todayActive.size());
                    overdueTasksCount.set(overdue.size());
                    completedTasksCount.set((int) stats.totalCompleted());
                    bestStreakCount.set(stats.longestStreak());
                    applyFilters();
                    isLoading.set(false);
                });
            } catch (Exception e) {
                LOGGER.severe("Failed to reload tasks: " + e.getMessage());
                runOnFxThread(() -> {
                    isLoading.set(false);
                    statusMessage.set("Failed to load tasks");
                });
            }
        });
    }

    private void runOnFxThread(Runnable action) {
        try {
            if (Platform.isFxApplicationThread()) {
                action.run();
            } else {
                Platform.runLater(action);
            }
        } catch (IllegalStateException e) {
            // Toolkit not initialized (e.g. headless unit tests)
            action.run();
        }
    }

    /**
     * Applies search query and category filtering in memory.
     */
    public void applyFilters() {
        String query = searchQuery.get();
        TaskCategory category = selectedCategoryFilter.get();

        List<Task> filtered = allLoadedTasks.stream()
                .filter(task -> {
                    if (query != null && !query.isBlank()) {
                        String q = query.trim().toLowerCase();
                        boolean matchTitle = task.getTitle().toLowerCase().contains(q);
                        boolean matchDesc = task.getDescription() != null && task.getDescription().toLowerCase().contains(q);
                        if (!matchTitle && !matchDesc) {
                            return false;
                        }
                    }
                    if (category != null && task.getCategory() != category) {
                        return false;
                    }
                    return true;
                })
                .toList();

        displayedTasks.setAll(filtered);
    }

    /**
     * Toggles task completion state.
     */
    public CompletableFuture<Void> toggleComplete(Task task) {
        if (task == null) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() -> {
            if (task.getStatus() == TaskStatus.COMPLETED) {
                task.setStatus(TaskStatus.PENDING);
                task.setCompletedAt(null);
                taskService.updateTask(task);
            } else {
                if (task.hasDuration() && task.getTimerRemainingSeconds() > 0) {
                    int sec = task.getTimerRemainingSeconds();
                    String timeStr = String.format("%02d:%02d", sec / 60, sec % 60);
                    throw new IllegalStateException("Focus timer still has " + timeStr + " remaining. Complete the timer before marking done.");
                }
                taskService.completeTask(task.getId());
            }
        }).thenCompose(v -> reloadTasks());
    }

    /**
     * Postpones/snoozes a task for a given number of minutes.
     */
    public CompletableFuture<Void> snoozeTask(Task task, int minutes) {
        if (task == null) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() -> {
            taskService.postponeTask(task.getId(), minutes);
        }).thenCompose(v -> reloadTasks());
    }

    /**
     * Creates or updates a task.
     */
    public CompletableFuture<Void> saveTask(Task task) {
        if (task == null) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() -> {
            if (taskService.getTask(task.getId()).isPresent()) {
                taskService.updateTask(task);
            } else {
                taskService.createTask(task);
            }
        }).thenCompose(v -> reloadTasks());
    }

    /**
     * Updates a task asynchronously in the background without triggering a full list reload.
     */
    public CompletableFuture<Void> saveTaskSilently(Task task) {
        if (task == null) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() -> {
            taskService.updateTask(task);
        });
    }

    /**
     * Soft-deletes a task.
     */
    public CompletableFuture<Void> deleteTask(Task task) {
        if (task == null) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() -> {
            taskService.deleteTask(task.getId());
        }).thenCompose(v -> reloadTasks());
    }

    /**
     * Restores a soft-deleted task.
     */
    public CompletableFuture<Void> restoreTask(String taskId) {
        if (taskId == null) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() -> {
            taskService.restoreTask(taskId);
        }).thenCompose(v -> reloadTasks());
    }

    // --- Property Getters ---

    public ObservableList<Task> getDisplayedTasks() {
        return displayedTasks;
    }

    public ObjectProperty<DashboardSection> activeSectionProperty() {
        return activeSection;
    }

    public ObjectProperty<TaskCategory> selectedCategoryFilterProperty() {
        return selectedCategoryFilter;
    }

    public StringProperty searchQueryProperty() {
        return searchQuery;
    }

    public IntegerProperty activeTasksCountProperty() {
        return activeTasksCount;
    }

    public IntegerProperty overdueTasksCountProperty() {
        return overdueTasksCount;
    }

    public IntegerProperty completedTasksCountProperty() {
        return completedTasksCount;
    }

    public IntegerProperty bestStreakCountProperty() {
        return bestStreakCount;
    }

    public BooleanProperty isLoadingProperty() {
        return isLoading;
    }

    public StringProperty statusMessageProperty() {
        return statusMessage;
    }
}
