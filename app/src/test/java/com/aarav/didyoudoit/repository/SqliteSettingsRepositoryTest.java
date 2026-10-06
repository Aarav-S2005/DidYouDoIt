package com.aarav.didyoudoit.repository;

import com.aarav.didyoudoit.model.AppSettings;
import com.aarav.didyoudoit.model.PersonalityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class SqliteSettingsRepositoryTest {

    private Path tempDbPath;
    private DatabaseManager dbManager;
    private SettingsRepository settingsRepository;

    @BeforeEach
    void setUp() throws IOException {
        tempDbPath = Files.createTempFile("didyoudoit_test_settings_", ".db");
        dbManager = new SqliteDatabaseManager(tempDbPath.toString());
        settingsRepository = new SqliteSettingsRepository(dbManager);
    }

    @AfterEach
    void tearDown() {
        dbManager.close();
        try {
            Files.deleteIfExists(tempDbPath);
        } catch (IOException ignored) {
        }
    }

    @Test
    @DisplayName("Default settings are populated on first retrieval")
    void testGetDefaultSettings() {
        AppSettings settings = settingsRepository.getSettings();
        assertNotNull(settings);
        assertEquals(PersonalityType.SARCASTIC, settings.getPersonalityType());
        assertFalse(settings.isQuietHoursEnabled());
        assertEquals(10, settings.getEscalationIntervalMinutes());
        assertEquals(15, settings.getDefaultSnoozeMinutes());
    }

    @Test
    @DisplayName("Save and reload updated settings")
    void testSaveAndReloadSettings() {
        AppSettings settings = settingsRepository.getSettings();
        settings.setPersonalityType(PersonalityType.AGGRESSIVE);
        settings.setQuietHoursEnabled(true);
        settings.setQuietHoursStart(LocalTime.of(23, 0));
        settings.setQuietHoursEnd(LocalTime.of(7, 30));
        settings.setAutoStartOnBoot(true);
        settings.setEscalationIntervalMinutes(5);
        settings.setDefaultSnoozeMinutes(20);

        settingsRepository.saveSettings(settings);

        AppSettings reloaded = settingsRepository.getSettings();
        assertEquals(PersonalityType.AGGRESSIVE, reloaded.getPersonalityType());
        assertTrue(reloaded.isQuietHoursEnabled());
        assertEquals(LocalTime.of(23, 0), reloaded.getQuietHoursStart());
        assertEquals(LocalTime.of(7, 30), reloaded.getQuietHoursEnd());
        assertTrue(reloaded.isAutoStartOnBoot());
        assertEquals(5, reloaded.getEscalationIntervalMinutes());
        assertEquals(20, reloaded.getDefaultSnoozeMinutes());
    }

    @Test
    @DisplayName("Quiet hours calculation logic")
    void testQuietHoursCalculation() {
        AppSettings settings = new AppSettings();
        settings.setQuietHoursEnabled(true);
        settings.setQuietHoursStart(LocalTime.of(22, 0));
        settings.setQuietHoursEnd(LocalTime.of(8, 0));

        assertTrue(settings.isInQuietHours(LocalTime.of(23, 0)));
        assertTrue(settings.isInQuietHours(LocalTime.of(2, 0)));
        assertTrue(settings.isInQuietHours(LocalTime.of(7, 59)));
        assertFalse(settings.isInQuietHours(LocalTime.of(8, 0)));
        assertFalse(settings.isInQuietHours(LocalTime.of(15, 0)));
        assertFalse(settings.isInQuietHours(LocalTime.of(21, 59)));
    }
}
