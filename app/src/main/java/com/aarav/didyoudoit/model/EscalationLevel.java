package com.aarav.didyoudoit.model;

/**
 * Escalation levels representing urgency and persistent nagging intensity.
 * Implements FR-05, FR-06.
 */
public enum EscalationLevel {
    INITIAL(1, "Initial Reminder"),
    NUDGE(2, "Persistent Nudge"),
    WARN(3, "Escalated Warning"),
    CRITICAL(4, "Critical Nagging");

    private final int level;
    private final String description;

    EscalationLevel(int level, String description) {
        this.level = level;
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    public static EscalationLevel fromLevel(int level) {
        for (EscalationLevel el : values()) {
            if (el.level == level) {
                return el;
            }
        }
        return INITIAL;
    }

    public EscalationLevel next() {
        return switch (this) {
            case INITIAL -> NUDGE;
            case NUDGE -> WARN;
            case WARN, CRITICAL -> CRITICAL;
        };
    }
}
