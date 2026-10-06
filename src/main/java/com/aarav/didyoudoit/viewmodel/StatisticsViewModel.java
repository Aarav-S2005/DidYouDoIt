package com.aarav.didyoudoit.viewmodel;

import com.aarav.didyoudoit.model.StreakInfo;
import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.model.TaskHistory;
import com.aarav.didyoudoit.model.TaskStatus;
import com.aarav.didyoudoit.repository.HistoryRepository;
import com.aarav.didyoudoit.service.StreakService;
import com.aarav.didyoudoit.service.TaskService;
import com.aarav.didyoudoit.util.ClockService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Presentation ViewModel for habit streaks, completion rates, accountability warnings,
 * and historical activity logs.
 * Adheres strictly to non-blocking UI thread execution.
 * Implements FR-02, FR-13, FR-14, FR-22, NFR-05, NFR-08.
 */
public class StatisticsViewModel {

    private static final Logger LOGGER = Logger.getLogger(StatisticsViewModel.class.getName());

    private final TaskService taskService;
    private final StreakService streakService;
    private final HistoryRepository historyRepository;
    private final ClockService clockService;

    // Observable presentation properties
    private final StringProperty completionRate7Days = new SimpleStringProperty("100%");
    private final StringProperty completionRate30Days = new SimpleStringProperty("100%");
    private final IntegerProperty totalCompleted = new SimpleIntegerProperty(0);
    private final IntegerProperty longestStreak = new SimpleIntegerProperty(0);
    private final IntegerProperty activeStreaksCount = new SimpleIntegerProperty(0);
    private final IntegerProperty missedCountThisWeek = new SimpleIntegerProperty(0);
    private final StringProperty missedHabitsAlert = new SimpleStringProperty("");
    private final BooleanProperty isLoading = new SimpleBooleanProperty(false);

    private final ObservableList<StreakInfo> habitStreaksList = FXCollections.observableArrayList();
    private final ObservableList<TaskHistory> historyLogList = FXCollections.observableArrayList();

    public StatisticsViewModel(TaskService taskService,
                               StreakService streakService,
                               HistoryRepository historyRepository,
                               ClockService clockService) {
        this.taskService = Objects.requireNonNull(taskService, "taskService cannot be null");
        this.streakService = Objects.requireNonNull(streakService, "streakService cannot be null");
        this.historyRepository = Objects.requireNonNull(historyRepository, "historyRepository cannot be null");
        this.clockService = Objects.requireNonNull(clockService, "clockService cannot be null");
    }

    /**
     * Asynchronously calculates metrics, streaks, and loads history.
     */
    public CompletableFuture<Void> reloadStats() {
        isLoading.set(true);
        return CompletableFuture.runAsync(() -> {
            try {
                LocalDate today = clockService.today();

                // 1. Streak Stats
                StreakService.StreakStats stats = streakService.getStreakStats();
                List<StreakInfo> streaks = streakService.getAllStreaks();

                // 2. 7-Day Completion Rate
                LocalDate start7 = today.minusDays(6);
                List<TaskHistory> history7 = historyRepository.findHistoryForDateRange(start7, today);
                List<Task> overdueTasks = taskService.getOverdueTasks();

                // Count tasks due in the last 7 days that remain overdue or completed
                long completed7Count = history7.size();
                long overdue7Count = overdueTasks.stream()
                        .filter(t -> t.getDueDateTime() != null && !t.getDueDateTime().toLocalDate().isBefore(start7))
                        .count();
                long total7 = completed7Count + overdue7Count;
                int rate7 = total7 > 0 ? (int) Math.round(((double) completed7Count / total7) * 100.0) : 100;

                // 3. 30-Day Completion Rate
                LocalDate start30 = today.minusDays(29);
                List<TaskHistory> history30 = historyRepository.findHistoryForDateRange(start30, today);
                long completed30Count = history30.size();
                long overdue30Count = overdueTasks.stream()
                        .filter(t -> t.getDueDateTime() != null && !t.getDueDateTime().toLocalDate().isBefore(start30))
                        .count();
                long total30 = completed30Count + overdue30Count;
                int rate30 = total30 > 0 ? (int) Math.round(((double) completed30Count / total30) * 100.0) : 100;

                // 4. Missed-task accountability alerts (FR-22)
                // "You've missed this 3 times this week" alert check
                Map<String, Long> overdueCountsByTitle = new HashMap<>();
                for (Task t : overdueTasks) {
                    overdueCountsByTitle.merge(t.getTitle(), 1L, Long::sum);
                }

                String alertMessage = "";
                for (Map.Entry<String, Long> entry : overdueCountsByTitle.entrySet()) {
                    if (entry.getValue() >= 3) {
                        alertMessage = "Accountability Alert: You've missed '" + entry.getKey() + "' " + entry.getValue() + " times this week!";
                        break;
                    }
                }
                if (alertMessage.isEmpty() && overdue7Count > 0) {
                    alertMessage = "You have " + overdue7Count + " overdue " + (overdue7Count == 1 ? "task" : "tasks") + " waiting for your attention.";
                }

                // 5. Recent History Log (last 50 items)
                List<TaskHistory> recentHistory = historyRepository.findRecentHistory(50);

                // Publish updates onto FX Thread
                String finalAlert = alertMessage;
                runOnFxThread(() -> {
                    completionRate7Days.set(rate7 + "%");
                    completionRate30Days.set(rate30 + "%");
                    totalCompleted.set((int) stats.totalCompleted());
                    longestStreak.set(stats.longestStreak());
                    activeStreaksCount.set(stats.activeStreaksCount());
                    missedCountThisWeek.set((int) overdue7Count);
                    missedHabitsAlert.set(finalAlert);

                    habitStreaksList.setAll(streaks);
                    historyLogList.setAll(recentHistory);
                    isLoading.set(false);
                });
            } catch (Exception e) {
                LOGGER.severe("Failed to reload statistics: " + e.getMessage());
                runOnFxThread(() -> isLoading.set(false));
            }
        });
    }

    /**
     * Undoes a completed task from history (FR-14), reopening the task and removing the history log record.
     */
    public CompletableFuture<Void> undoCompletion(TaskHistory entry) {
        if (entry == null) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() -> {
            Optional<Task> optTask = taskService.getTask(entry.getTaskId());
            if (optTask.isPresent()) {
                Task t = optTask.get();
                t.setStatus(TaskStatus.PENDING);
                t.setCompletedAt(null);
                taskService.updateTask(t);
            }
            historyRepository.deleteHistory(entry.getId());
        }).thenCompose(v -> reloadStats());
    }

    private void runOnFxThread(Runnable action) {
        try {
            if (Platform.isFxApplicationThread()) {
                action.run();
            } else {
                Platform.runLater(action);
            }
        } catch (IllegalStateException e) {
            // Headless unit tests without JavaFX toolkit initialized
            action.run();
        }
    }

    // Properties accessors
    public StringProperty completionRate7DaysProperty() { return completionRate7Days; }
    public StringProperty completionRate30DaysProperty() { return completionRate30Days; }
    public IntegerProperty totalCompletedProperty() { return totalCompleted; }
    public IntegerProperty longestStreakProperty() { return longestStreak; }
    public IntegerProperty activeStreaksCountProperty() { return activeStreaksCount; }
    public IntegerProperty missedCountThisWeekProperty() { return missedCountThisWeek; }
    public StringProperty missedHabitsAlertProperty() { return missedHabitsAlert; }
    public BooleanProperty isLoadingProperty() { return isLoading; }

    public ObservableList<StreakInfo> getHabitStreaksList() { return habitStreaksList; }
    public ObservableList<TaskHistory> getHistoryLogList() { return historyLogList; }
}
