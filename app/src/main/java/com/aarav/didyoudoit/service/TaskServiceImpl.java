package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.*;
import com.aarav.didyoudoit.repository.HistoryRepository;
import com.aarav.didyoudoit.repository.TaskRepository;
import com.aarav.didyoudoit.util.ClockService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Implementation of {@link TaskService}.
 * Implements FR-01, FR-02, FR-08, FR-09, FR-12, FR-13, FR-14, FR-21.
 */
public class TaskServiceImpl implements TaskService {

    private static final Logger LOGGER = Logger.getLogger(TaskServiceImpl.class.getName());

    private final TaskRepository taskRepository;
    private final HistoryRepository historyRepository;
    private final StreakService streakService;
    private final RecurringTaskEngine recurringTaskEngine;
    private final ClockService clockService;

    public TaskServiceImpl(TaskRepository taskRepository,
                           HistoryRepository historyRepository,
                           StreakService streakService,
                           RecurringTaskEngine recurringTaskEngine,
                           ClockService clockService) {
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository cannot be null");
        this.historyRepository = Objects.requireNonNull(historyRepository, "historyRepository cannot be null");
        this.streakService = Objects.requireNonNull(streakService, "streakService cannot be null");
        this.recurringTaskEngine = Objects.requireNonNull(recurringTaskEngine, "recurringTaskEngine cannot be null");
        this.clockService = Objects.requireNonNull(clockService, "clockService cannot be null");
    }

    @Override
    public Task createTask(Task task) {
        Objects.requireNonNull(task, "task cannot be null");

        if (task.getRecurrenceRule() != null && task.getRecurrenceRule().getType().isRecurring()) {
            // Configure recurring template
            task.setTemplate(true);
            taskRepository.save(task);
            LOGGER.info("Created recurring template: " + task.getTitle());

            // Generate instance for today if it applies
            LocalDate today = clockService.today();
            if (task.getRecurrenceRule().appliesToDate(today)) {
                if (!taskRepository.hasInstanceForTemplateOnDate(task.getId(), today)) {
                    Task todayInstance = recurringTaskEngine.createInstanceFromTemplate(task, today);
                    return taskRepository.save(todayInstance);
                }
            }
            return task;
        }

        task.setTemplate(false);
        return taskRepository.save(task);
    }

    @Override
    public Task updateTask(Task task) {
        Objects.requireNonNull(task, "task cannot be null");
        if (task.getStatus() == TaskStatus.COMPLETED && task.hasDuration() && task.getTimerRemainingSeconds() > 0) {
            throw new IllegalStateException("Cannot complete task while focus timer has time remaining ("
                    + task.getTimerRemainingSeconds() + "s left).");
        }
        return taskRepository.save(task);
    }

    @Override
    public void completeTask(String taskId) {
        Optional<Task> opt = taskRepository.findById(taskId);
        if (opt.isEmpty()) {
            LOGGER.warning("Attempted to complete non-existent task: " + taskId);
            return;
        }

        Task task = opt.get();
        if (task.getStatus() == TaskStatus.COMPLETED) {
            return;
        }

        // Do not allow task to be marked done if timer is not zero or timer is paused but not zero
        if (task.hasDuration() && task.getTimerRemainingSeconds() > 0) {
            LOGGER.warning("Cannot complete task '" + task.getTitle() + "': focus timer has "
                    + task.getTimerRemainingSeconds() + "s remaining.");
            throw new IllegalStateException("Cannot complete task while focus timer has time remaining ("
                    + task.getTimerRemainingSeconds() + "s left).");
        }

        LocalDateTime now = clockService.now();
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(now);
        task.setTimerActive(false);
        taskRepository.save(task);

        // Record history snapshot
        TaskHistory history = TaskHistory.fromCompletedTask(task);
        historyRepository.logCompletion(history);

        // Update streaks if this task belongs to a recurring template
        streakService.recordTaskCompletion(task);

        LOGGER.info("Completed task '" + task.getTitle() + "' and logged history/streak.");
    }

    @Override
    public void postponeTask(String taskId, int minutes) {
        Optional<Task> opt = taskRepository.findById(taskId);
        if (opt.isEmpty()) {
            return;
        }

        Task task = opt.get();
        int snoozeMins = Math.max(1, minutes);
        LocalDateTime postponedTime = clockService.now().plusMinutes(snoozeMins);

        task.setStatus(TaskStatus.POSTPONED);
        task.setPostponedUntil(postponedTime);
        taskRepository.save(task);
        LOGGER.info("Postponed task '" + task.getTitle() + "' by " + snoozeMins + " minutes.");
    }

    @Override
    public void deleteTask(String taskId) {
        taskRepository.delete(taskId);
    }

    @Override
    public void restoreTask(String taskId) {
        taskRepository.restore(taskId);
    }

    @Override
    public void hardDeleteTask(String taskId) {
        taskRepository.hardDelete(taskId);
    }

    @Override
    public Optional<Task> getTask(String taskId) {
        return taskRepository.findById(taskId);
    }

    @Override
    public List<Task> getTodayTasks() {
        return taskRepository.findTasksForDate(clockService.today());
    }

    @Override
    public List<Task> getActiveTasks() {
        return taskRepository.findActiveTasks();
    }

    @Override
    public List<Task> getOverdueTasks() {
        return taskRepository.findOverdueTasks(clockService.now());
    }

    @Override
    public List<Task> searchTasks(String query, TaskCategory category, Priority priority, TaskStatus status) {
        return taskRepository.searchAndFilter(query, category, priority, status);
    }

    @Override
    public void recordNag(String taskId) {
        Optional<Task> opt = taskRepository.findById(taskId);
        if (opt.isEmpty()) {
            return;
        }

        Task task = opt.get();
        task.incrementNagCount();
        if (task.getStatus() == TaskStatus.PENDING && task.isOverdue(clockService.now())) {
            task.setStatus(TaskStatus.OVERDUE);
        }
        taskRepository.save(task);
    }
}
