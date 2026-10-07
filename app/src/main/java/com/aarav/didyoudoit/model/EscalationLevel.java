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

    /**
     * Resolves the escalation intensity level based on persistent nag count:
     * - INITIAL:  nagCount < 2 (0, 1)
     * - NUDGE:    nagCount 2 to 3
     * - WARN:     nagCount 4 to 6
     * - CRITICAL: nagCount >= 7
     */
    public static EscalationLevel fromNagCount(int nagCount) {
        if (nagCount >= 7) {
            return CRITICAL;
        } else if (nagCount >= 4) {
            return WARN;
        } else if (nagCount >= 2) {
            return NUDGE;
        } else {
            return INITIAL;
        }
    }

    public EscalationLevel next() {
        return switch (this) {
            case INITIAL -> NUDGE;
            case NUDGE -> WARN;
            case WARN, CRITICAL -> CRITICAL;
        };
    }
}
