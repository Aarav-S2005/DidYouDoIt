package com.aarav.didyoudoit.model;

/**
 * Lifecycle status of a task.
 * Implements FR-01, FR-02.
 */
public enum TaskStatus {
    PENDING("Pending"),
    COMPLETED("Completed"),
    POSTPONED("Postponed"),
    OVERDUE("Overdue"),
    DELETED("Deleted");

    private final String displayName;

    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this == PENDING || this == POSTPONED || this == OVERDUE;
    }
}
