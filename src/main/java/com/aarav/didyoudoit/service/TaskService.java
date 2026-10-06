package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.Priority;
import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.model.TaskCategory;
import com.aarav.didyoudoit.model.TaskStatus;

import java.util.List;
import java.util.Optional;

/**
 * Service managing task lifecycle operations, status transitions,
 * history logging, and habit streaks.
 * Implements FR-01, FR-02, FR-08, FR-09, FR-12, FR-21.
 */
public interface TaskService {

    /**
     * Creates and persists a new task.
     * If the task is recurring, creates the template and spawns today's instance if applicable.
     */
    Task createTask(Task task);

    /**
     * Updates an existing task.
     */
    Task updateTask(Task task);

    /**
     * Marks a task completed, logs completion history, and updates streaks if applicable.
     * Implements FR-02, FR-13, FR-14.
     */
    void completeTask(String taskId);

    /**
     * Postpones/snoozes a task for a specified duration in minutes.
     * Implements FR-02, FR-09.
     */
    void postponeTask(String taskId, int minutes);

    /**
     * Soft-deletes a task.
     * Implements FR-02.
     */
    void deleteTask(String taskId);

    /**
     * Restores a previously soft-deleted task.
     * Implements FR-02.
     */
    void restoreTask(String taskId);

    /**
     * Permanently deletes a task.
     */
    void hardDeleteTask(String taskId);

    /**
     * Retrieves a task by ID.
     */
    Optional<Task> getTask(String taskId);

    /**
     * Retrieves tasks scheduled for today.
     * Implements FR-12.
     */
    List<Task> getTodayTasks();

    /**
     * Retrieves active tasks (pending, postponed, overdue).
     */
    List<Task> getActiveTasks();

    /**
     * Retrieves overdue tasks based on current clock.
     * Implements FR-05, FR-12.
     */
    List<Task> getOverdueTasks();

    /**
     * Searches and filters tasks.
     * Implements FR-21.
     */
    List<Task> searchTasks(String query, TaskCategory category, Priority priority, TaskStatus status);

    /**
     * Increments the nagging count and escalation level for a task.
     * Implements FR-05, FR-06.
     */
    void recordNag(String taskId);
}
