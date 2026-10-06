package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.StreakInfo;
import com.aarav.didyoudoit.model.Task;

import java.util.List;

/**
 * Service managing habit completion streaks and statistics.
 * Implements FR-13, FR-14, FR-22.
 */
public interface StreakService {

    /**
     * Summary streak statistics for dashboard and progress analytics.
     */
    record StreakStats(long totalCompleted, long totalOverdue, int activeStreaksCount, int longestStreak) {}

    /**
     * Retrieves streak info for a recurring template task.
     */
    StreakInfo getStreak(String templateId, String taskTitle);

    /**
     * Records task completion and updates streak if task belongs to a recurring template.
     */
    void recordTaskCompletion(Task task);

    /**
     * Retrieves all tracked streaks.
     */
    List<StreakInfo> getAllStreaks();

    /**
     * Computes aggregate streak and completion statistics.
     */
    StreakStats getStreakStats();
}
