package com.aarav.didyoudoit.repository;

import com.aarav.didyoudoit.model.StreakInfo;
import com.aarav.didyoudoit.model.TaskHistory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data access operations for task completion history and streak statistics.
 * Implements FR-13, FR-14, FR-22, FR-23.
 */
public interface HistoryRepository {

    /**
     * Records a completed task snapshot into history.
     */
    void logCompletion(TaskHistory history);

    /**
     * Retrieves the most recent completed task history records.
     */
    List<TaskHistory> findRecentHistory(int limit);

    /**
     * Retrieves history records completed between start and end dates inclusive.
     */
    List<TaskHistory> findHistoryForDateRange(LocalDate start, LocalDate end);

    /**
     * Retrieves streak information for a recurring task template.
     */
    Optional<StreakInfo> findStreak(String templateId);

    /**
     * Saves or updates streak info for a recurring template.
     */
    void saveStreak(StreakInfo streakInfo);

    /**
     * Retrieves all active streak records.
     */
    List<StreakInfo> findAllStreaks();

    /**
     * Counts total completed tasks in history.
     */
    long countCompletedTasks();

    /**
     * Counts completed tasks that were overdue when completed.
     */
    long countOverdueCompletions();

    /**
     * Deletes a specific history record by its history ID.
     */
    void deleteHistory(String historyId);

    /**
     * Deletes history records associated with a specific task ID.
     */
    void deleteHistoryByTaskId(String taskId);
}
