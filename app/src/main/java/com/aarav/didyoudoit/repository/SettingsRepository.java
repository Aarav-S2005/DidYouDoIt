package com.aarav.didyoudoit.repository;

import com.aarav.didyoudoit.model.AppSettings;

/**
 * Data access operations for application preferences and configuration.
 * Implements FR-15, FR-17, FR-20, FR-23, FR-25.
 */
public interface SettingsRepository {

    /**
     * Loads the current application settings, returning defaults if not yet initialized.
     */
    AppSettings getSettings();

    /**
     * Persists updated application settings.
     */
    void saveSettings(AppSettings settings);
}
