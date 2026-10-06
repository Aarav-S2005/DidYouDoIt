package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.AppSettings;
import com.aarav.didyoudoit.model.PersonalityType;

/**
 * Service managing application settings, quiet hours, and reminder suppression.
 * Implements FR-15, FR-17, FR-20, FR-23, FR-25.
 */
public interface SettingsService {

    /**
     * Retrieves current cached/persisted app settings.
     */
    AppSettings getSettings();

    /**
     * Persists updated settings.
     */
    void updateSettings(AppSettings settings);

    /**
     * Checks if notifications/reminders are currently suppressed due to quiet hours or manual pause.
     */
    boolean areRemindersSuppressed();

    /**
     * Pauses all reminders for a specific duration in minutes (or indefinitely if 0).
     */
    void pauseReminders(int durationMinutes);

    /**
     * Resumes reminders immediately.
     */
    void resumeReminders();

    /**
     * Updates active personality tone.
     */
    void setPersonality(PersonalityType personality);
}
