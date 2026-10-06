package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.AppSettings;
import com.aarav.didyoudoit.model.PersonalityType;
import com.aarav.didyoudoit.repository.DatabaseManager;
import com.aarav.didyoudoit.repository.SettingsRepository;
import com.aarav.didyoudoit.repository.SqliteDatabaseManager;
import com.aarav.didyoudoit.repository.SqliteSettingsRepository;
import com.aarav.didyoudoit.util.TestClockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class SettingsServiceImplTest {

    private Path tempDbPath;
    private DatabaseManager dbManager;
    private SettingsRepository settingsRepository;
    private TestClockService clockService;
    private SettingsService settingsService;

    @BeforeEach
    void setUp() throws IOException {
        tempDbPath = Files.createTempFile("didyoudoit_service_settings_", ".db");
        dbManager = new SqliteDatabaseManager(tempDbPath.toString());
        settingsRepository = new SqliteSettingsRepository(dbManager);
        clockService = new TestClockService(LocalDateTime.of(2026, 10, 6, 14, 0));
        settingsService = new SettingsServiceImpl(settingsRepository, clockService);
    }

    @AfterEach
    void tearDown() {
        dbManager.close();
        try {
            Files.deleteIfExists(tempDbPath);
        } catch (IOException ignored) {}
    }

    @Test
    @DisplayName("Pause reminders and automatic expiration")
    void testPauseReminders() {
        assertFalse(settingsService.areRemindersSuppressed());

        // Pause for 30 minutes
        settingsService.pauseReminders(30);
        assertTrue(settingsService.areRemindersSuppressed());

        // Advance 15 minutes -> still suppressed
        clockService.advanceMinutes(15);
        assertTrue(settingsService.areRemindersSuppressed());

        // Advance 20 more minutes (total 35m) -> pause expires, unsuppressed
        clockService.advanceMinutes(20);
        assertFalse(settingsService.areRemindersSuppressed());
    }

    @Test
    @DisplayName("Quiet hours suppression")
    void testQuietHoursSuppression() {
        AppSettings s = settingsService.getSettings();
        s.setQuietHoursEnabled(true);
        s.setQuietHoursStart(LocalTime.of(22, 0));
        s.setQuietHoursEnd(LocalTime.of(8, 0));
        settingsService.updateSettings(s);

        // 14:00 -> not suppressed
        assertFalse(settingsService.areRemindersSuppressed());

        // Travel to 23:30 -> suppressed
        clockService.setTime(LocalDateTime.of(2026, 10, 6, 23, 30));
        assertTrue(settingsService.areRemindersSuppressed());

        // Travel to 07:45 -> suppressed
        clockService.setTime(LocalDateTime.of(2026, 10, 7, 7, 45));
        assertTrue(settingsService.areRemindersSuppressed());

        // Travel to 08:05 -> not suppressed
        clockService.setTime(LocalDateTime.of(2026, 10, 7, 8, 5));
        assertFalse(settingsService.areRemindersSuppressed());
    }

    @Test
    @DisplayName("Set personality updates settings")
    void testSetPersonality() {
        settingsService.setPersonality(PersonalityType.AGGRESSIVE);
        assertEquals(PersonalityType.AGGRESSIVE, settingsService.getSettings().getPersonalityType());
    }
}
