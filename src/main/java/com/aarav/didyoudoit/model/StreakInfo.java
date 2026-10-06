package com.aarav.didyoudoit.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Tracks consecutive completion streaks for recurring tasks.
 * Implements FR-14.
 */
public class StreakInfo {

    private final String templateId;
    private final String taskTitle;
    private int currentStreak;
    private int bestStreak;
    private LocalDate lastCompletedDate;

    public StreakInfo(String templateId, String taskTitle, int currentStreak, int bestStreak, LocalDate lastCompletedDate) {
        this.templateId = Objects.requireNonNull(templateId, "templateId cannot be null");
        this.taskTitle = Objects.requireNonNullElse(taskTitle, "");
        this.currentStreak = Math.max(0, currentStreak);
        this.bestStreak = Math.max(0, bestStreak);
        this.lastCompletedDate = lastCompletedDate;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public int getBestStreak() {
        return bestStreak;
    }

    public LocalDate getLastCompletedDate() {
        return lastCompletedDate;
    }

    /**
     * Updates streak based on a completion for a given date.
     */
    public void recordCompletion(LocalDate completionDate) {
        if (completionDate == null) {
            return;
        }

        if (lastCompletedDate == null) {
            currentStreak = 1;
            bestStreak = Math.max(bestStreak, currentStreak);
            lastCompletedDate = completionDate;
            return;
        }

        if (lastCompletedDate.equals(completionDate)) {
            // Already counted for today
            return;
        }

        if (lastCompletedDate.plusDays(1).equals(completionDate)) {
            // Consecutive day
            currentStreak++;
        } else if (completionDate.isAfter(lastCompletedDate.plusDays(1))) {
            // Streak broken
            currentStreak = 1;
        }

        bestStreak = Math.max(bestStreak, currentStreak);
        lastCompletedDate = completionDate;
    }
}
