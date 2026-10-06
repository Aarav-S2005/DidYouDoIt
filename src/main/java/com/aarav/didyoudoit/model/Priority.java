package com.aarav.didyoudoit.model;

/**
 * Priority levels for tasks affecting reminder urgency and escalation.
 * Implements FR-10.
 */
public enum Priority {
    LOW(1, "Low", "badge-low"),
    MEDIUM(2, "Medium", "badge-medium"),
    HIGH(3, "High", "badge-high"),
    URGENT(4, "Urgent", "badge-urgent");

    private final int level;
    private final String displayName;
    private final String styleClass;

    Priority(int level, String displayName, String styleClass) {
        this.level = level;
        this.displayName = displayName;
        this.styleClass = styleClass;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getStyleClass() {
        return styleClass;
    }

    public static Priority fromLevel(int level) {
        for (Priority p : values()) {
            if (p.level == level) {
                return p;
            }
        }
        return MEDIUM;
    }
}
