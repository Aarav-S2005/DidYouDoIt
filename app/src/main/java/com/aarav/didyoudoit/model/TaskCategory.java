package com.aarav.didyoudoit.model;

/**
 * Task category for grouping user activities.
 * Implements FR-11.
 */
public enum TaskCategory {
    WORK("Work", "💼"),
    STUDY("Study", "📚"),
    FITNESS("Fitness", "🏃"),
    PERSONAL("Personal", "🌱"),
    CHORES("Chores", "🧹"),
    HEALTH("Health", "💊"),
    OTHER("Other", "📌");

    private final String displayName;
    private final String icon;

    TaskCategory(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public static TaskCategory fromString(String text) {
        if (text == null || text.isBlank()) {
            return OTHER;
        }
        for (TaskCategory c : values()) {
            if (c.name().equalsIgnoreCase(text.trim()) || c.displayName.equalsIgnoreCase(text.trim())) {
                return c;
            }
        }
        return OTHER;
    }
}
