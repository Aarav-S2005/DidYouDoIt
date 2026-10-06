package com.aarav.didyoudoit.repository;

import com.aarav.didyoudoit.model.Priority;
import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.model.TaskCategory;
import com.aarav.didyoudoit.model.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Data access operations for {@link Task} entities.
 * Implements FR-01, FR-02, FR-03, FR-21, FR-23.
 */
public interface TaskRepository {

    /**
     * Saves or updates a task.
     *
     * @param task task to save
     * @return the persisted task
     */
    Task save(Task task);

    /**
     * Finds a task by unique ID.
     */
    Optional<Task> findById(String id);

    /**
     * Retrieves all tasks including completed and deleted.
     */
    List<Task> findAll();

    /**
     * Retrieves active tasks (non-templates, not completed, not deleted).
     */
    List<Task> findActiveTasks();

    /**
     * Retrieves tasks scheduled for or due on a specific date.
     */
    List<Task> findTasksForDate(LocalDate date);

    /**
     * Retrieves incomplete tasks whose effective due date is in the past.
     */
    List<Task> findOverdueTasks(LocalDateTime referenceTime);

    /**
     * Retrieves all recurring task template definitions.
     */
    List<Task> findRecurringTemplates();

    /**
     * Checks if a daily instance has already been generated for a template on a given date.
     */
    boolean hasInstanceForTemplateOnDate(String templateId, LocalDate date);

    /**
     * Searches and filters tasks by query text, category, priority, and status.
     * Implements FR-21.
     */
    List<Task> searchAndFilter(String query, TaskCategory category, Priority priority, TaskStatus status);

    /**
     * Soft-deletes a task (marks status as DELETED).
     * Implements FR-02.
     */
    void delete(String id);

    /**
     * Restores a previously soft-deleted task.
     * Implements FR-02.
     */
    void restore(String id);

    /**
     * Permanently deletes a task record from database.
     */
    void hardDelete(String id);
}
