package com.aarav.didyoudoit.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Application-wide user preferences and system settings.
 * Implements FR-15, FR-17, FR-20, FR-23, FR-25.
 */
public class AppSettings {

    private PersonalityType personalityType;
    private boolean quietHoursEnabled;
    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;
    private boolean remindersPaused;
    private LocalDateTime remindersPausedUntil;
    private boolean autoStartOnBoot;
    private boolean persistentNaggingEnabled;
    private int escalationIntervalMinutes;
    private int defaultSnoozeMinutes;
    private LocalDateTime updatedAt;

    public AppSettings() {
        this.personalityType = PersonalityType.SARCASTIC;
        this.quietHoursEnabled = false;
        this.quietHoursStart = LocalTime.of(22, 0);
        this.quietHoursEnd = LocalTime.of(8, 0);
        this.remindersPaused = false;
        this.remindersPausedUntil = null;
        this.autoStartOnBoot = false;
        this.persistentNaggingEnabled = true;
        this.escalationIntervalMinutes = 10;
        this.defaultSnoozeMinutes = 15;
        this.updatedAt = LocalDateTime.now();
    }

    public static AppSettings createDefault() {
        return new AppSettings();
    }

    public PersonalityType getPersonalityType() {
        return personalityType;
    }

    public void setPersonalityType(PersonalityType personalityType) {
        this.personalityType = Objects.requireNonNullElse(personalityType, PersonalityType.SARCASTIC);
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isQuietHoursEnabled() {
        return quietHoursEnabled;
    }

    public void setQuietHoursEnabled(boolean quietHoursEnabled) {
        this.quietHoursEnabled = quietHoursEnabled;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalTime getQuietHoursStart() {
        return quietHoursStart;
    }

    public void setQuietHoursStart(LocalTime quietHoursStart) {
        this.quietHoursStart = Objects.requireNonNullElse(quietHoursStart, LocalTime.of(22, 0));
        this.updatedAt = LocalDateTime.now();
    }

    public LocalTime getQuietHoursEnd() {
        return quietHoursEnd;
    }

    public void setQuietHoursEnd(LocalTime quietHoursEnd) {
        this.quietHoursEnd = Objects.requireNonNullElse(quietHoursEnd, LocalTime.of(8, 0));
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isRemindersPaused() {
        if (!remindersPaused) {
            return false;
        }
        if (remindersPausedUntil != null && LocalDateTime.now().isAfter(remindersPausedUntil)) {
            remindersPaused = false;
            remindersPausedUntil = null;
            return false;
        }
        return true;
    }

    public void setRemindersPaused(boolean remindersPaused) {
        this.remindersPaused = remindersPaused;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getRemindersPausedUntil() {
        return remindersPausedUntil;
    }

    public void setRemindersPausedUntil(LocalDateTime remindersPausedUntil) {
        this.remindersPausedUntil = remindersPausedUntil;
        this.remindersPaused = (remindersPausedUntil != null);
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isAutoStartOnBoot() {
        return autoStartOnBoot;
    }

    public void setAutoStartOnBoot(boolean autoStartOnBoot) {
        this.autoStartOnBoot = autoStartOnBoot;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isPersistentNaggingEnabled() {
        return persistentNaggingEnabled;
    }

    public void setPersistentNaggingEnabled(boolean persistentNaggingEnabled) {
        this.persistentNaggingEnabled = persistentNaggingEnabled;
        this.updatedAt = LocalDateTime.now();
    }

    public int getEscalationIntervalMinutes() {
        return escalationIntervalMinutes;
    }

    public void setEscalationIntervalMinutes(int escalationIntervalMinutes) {
        this.escalationIntervalMinutes = Math.max(1, escalationIntervalMinutes);
        this.updatedAt = LocalDateTime.now();
    }

    public int getDefaultSnoozeMinutes() {
        return defaultSnoozeMinutes;
    }

    public void setDefaultSnoozeMinutes(int defaultSnoozeMinutes) {
        this.defaultSnoozeMinutes = Math.max(1, defaultSnoozeMinutes);
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Checks if current time is within configured quiet hours.
     */
    public boolean isInQuietHours(LocalTime time) {
        if (!quietHoursEnabled || quietHoursStart == null || quietHoursEnd == null) {
            return false;
        }
        if (quietHoursStart.equals(quietHoursEnd)) {
            return false;
        }
        if (quietHoursStart.isBefore(quietHoursEnd)) {
            return !time.isBefore(quietHoursStart) && time.isBefore(quietHoursEnd);
        } else {
            // Quiet hours cross midnight (e.g. 22:00 -> 08:00)
            return !time.isBefore(quietHoursStart) || time.isBefore(quietHoursEnd);
        }
    }
}
