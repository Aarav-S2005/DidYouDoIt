package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.AppSettings;
import com.aarav.didyoudoit.model.PersonalityType;
import com.aarav.didyoudoit.repository.SettingsRepository;
import com.aarav.didyoudoit.util.ClockService;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Implementation of {@link SettingsService}.
 * Implements FR-15, FR-17, FR-20, FR-23, FR-25.
 */
public class SettingsServiceImpl implements SettingsService {

    private final SettingsRepository settingsRepository;
    private final ClockService clockService;

    public SettingsServiceImpl(SettingsRepository settingsRepository, ClockService clockService) {
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository cannot be null");
        this.clockService = Objects.requireNonNull(clockService, "clockService cannot be null");
    }

    @Override
    public AppSettings getSettings() {
        return settingsRepository.getSettings();
    }

    @Override
    public void updateSettings(AppSettings settings) {
        Objects.requireNonNull(settings, "settings cannot be null");
        settingsRepository.saveSettings(settings);
    }

    @Override
    public boolean areRemindersSuppressed() {
        AppSettings settings = getSettings();

        // 1. Check if manually paused
        if (settings.isRemindersPaused()) {
            LocalDateTime pauseUntil = settings.getRemindersPausedUntil();
            if (pauseUntil == null || clockService.now().isBefore(pauseUntil)) {
                return true;
            } else {
                // Expired pause
                resumeReminders();
            }
        }

        // 2. Check quiet hours
        return settings.isInQuietHours(clockService.currentTime());
    }

    @Override
    public void pauseReminders(int durationMinutes) {
        AppSettings settings = getSettings();
        settings.setRemindersPaused(true);
        if (durationMinutes > 0) {
            settings.setRemindersPausedUntil(clockService.now().plusMinutes(durationMinutes));
        } else {
            settings.setRemindersPausedUntil(null);
        }
        updateSettings(settings);
    }

    @Override
    public void resumeReminders() {
        AppSettings settings = getSettings();
        settings.setRemindersPaused(false);
        settings.setRemindersPausedUntil(null);
        updateSettings(settings);
    }

    @Override
    public void setPersonality(PersonalityType personality) {
        AppSettings settings = getSettings();
        settings.setPersonalityType(personality);
        updateSettings(settings);
    }
}
