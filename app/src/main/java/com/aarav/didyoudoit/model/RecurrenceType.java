package com.aarav.didyoudoit.model;

/**
 * Recurrence schedule pattern for recurring tasks.
 * Implements FR-01, FR-03.
 */
public enum RecurrenceType {
    NONE("None"),
    DAILY("Every Day"),
    WEEKDAYS("Weekdays (Mon-Fri)"),
    WEEKENDS("Weekends (Sat-Sun)"),
    WEEKLY("Weekly");

    private final String displayName;

    RecurrenceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isRecurring() {
        return this != NONE;
    }
}
