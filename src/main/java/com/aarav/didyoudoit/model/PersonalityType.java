package com.aarav.didyoudoit.model;

/**
 * Reminder personalities dictating tone and message style.
 * Implements FR-15, BR-02, BR-12.
 */
public enum PersonalityType {
    GENTLE("Gentle", "Warm & encouraging reminders with kind nudges.", "🌸"),
    STRICT("Strict", "Firm, direct, and focused on discipline.", "📏"),
    SARCASTIC("Sarcastic", "Witty, teasing commentary on your procrastination.", "😏"),
    AGGRESSIVE("Aggressive", "Relentless, drill-sergeant energy that won't let you quit.", "🔥");

    private final String displayName;
    private final String description;
    private final String emoji;

    PersonalityType(String displayName, String description, String emoji) {
        this.displayName = displayName;
        this.description = description;
        this.emoji = emoji;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getEmoji() {
        return emoji;
    }

    public static PersonalityType fromString(String name) {
        if (name == null || name.isBlank()) {
            return SARCASTIC;
        }
        for (PersonalityType p : values()) {
            if (p.name().equalsIgnoreCase(name.trim()) || p.displayName.equalsIgnoreCase(name.trim())) {
                return p;
            }
        }
        return SARCASTIC;
    }
}
