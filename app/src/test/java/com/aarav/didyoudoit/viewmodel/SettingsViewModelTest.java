package com.aarav.didyoudoit.viewmodel;

import com.aarav.didyoudoit.model.PersonalityType;
import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.model.TaskCategory;
import com.aarav.didyoudoit.repository.*;
import com.aarav.didyoudoit.service.*;
import com.aarav.didyoudoit.util.TestClockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class SettingsViewModelTest {

    private Path tempDbPath;
    private DatabaseManager dbManager;
    private SettingsRepository settingsRepository;
    private TaskRepository taskRepository;
    private HistoryRepository historyRepository;
    private TestClockService clockService;
    private SettingsService settingsService;
    private PersonalityMessageService messageService;
    private DataBackupService backupService;
    private SettingsViewModel viewModel;

    @BeforeEach
    void setUp() throws IOException {
        tempDbPath = Files.createTempFile("didyoudoit_settings_test_", ".db");
        dbManager = new SqliteDatabaseManager(tempDbPath.toString());
        settingsRepository = new SqliteSettingsRepository(dbManager);
        taskRepository = new SqliteTaskRepository(dbManager);
        historyRepository = new SqliteHistoryRepository(dbManager);
        clockService = new TestClockService(LocalDateTime.of(2026, 10, 6, 14, 0));
        settingsService = new SettingsServiceImpl(settingsRepository, clockService);
        messageService = new PersonalityMessageServiceImpl();
        backupService = new JsonDataBackupServiceImpl(taskRepository, settingsRepository, historyRepository);

        viewModel = new SettingsViewModel(settingsService, messageService, backupService);
    }

    @AfterEach
    void tearDown() {
        dbManager.close();
        try {
            Files.deleteIfExists(tempDbPath);
        } catch (IOException ignored) {}
    }

    @Test
    @DisplayName("Switching personality updates preferences and live quote preview (FR-15, FR-16)")
    void testPersonalitySelectionAndLiveQuote() {
        viewModel.setPersonality(PersonalityType.GENTLE);

        assertEquals(PersonalityType.GENTLE, viewModel.personalityTypeProperty().get());
        assertEquals(PersonalityType.GENTLE, settingsService.getSettings().getPersonalityType());

        String preview = viewModel.previewSampleQuoteProperty().get();
        assertNotNull(preview);
        assertFalse(preview.isBlank());
        assertTrue(preview.contains("Gentle") || preview.contains("Reminder") || preview.contains("Initial"));
    }

    @Test
    @DisplayName("Configuring quiet hours persists start and end times (FR-17)")
    void testQuietHoursConfiguration() {
        viewModel.setQuietHours(true, LocalTime.of(23, 0), LocalTime.of(7, 0));

        assertTrue(viewModel.quietHoursEnabledProperty().get());
        assertEquals(LocalTime.of(23, 0), viewModel.quietHoursStartProperty().get());
        assertEquals(LocalTime.of(7, 0), viewModel.quietHoursEndProperty().get());

        var reloaded = settingsService.getSettings();
        assertTrue(reloaded.isQuietHoursEnabled());
        assertEquals(LocalTime.of(23, 0), reloaded.getQuietHoursStart());
        assertEquals(LocalTime.of(7, 0), reloaded.getQuietHoursEnd());
    }

    @Test
    @DisplayName("Pause reminders and resume toggles suppression state (FR-17)")
    void testPauseAndResumeReminders() {
        viewModel.pauseReminders(30);
        assertTrue(viewModel.remindersPausedProperty().get());

        viewModel.resumeReminders();
        assertFalse(viewModel.remindersPausedProperty().get());
    }

    @Test
    @DisplayName("JSON backup export and import restores tasks and settings (FR-25)")
    void testDataBackupRoundTrip() throws IOException {
        // Create a task to export
        Task task = Task.builder()
                .title("Prepare Presentation Slides")
                .description("Key slides for stakeholders")
                .category(TaskCategory.WORK)
                .build();
        taskRepository.save(task);

        // Customize setting
        viewModel.setPersonality(PersonalityType.AGGRESSIVE);

        // Export to temp file
        File backupFile = File.createTempFile("didyoudoit_backup_test_", ".json");
        try {
            viewModel.exportBackupAsync(backupFile).join();
            assertTrue(backupFile.exists());
            assertTrue(backupFile.length() > 0);

            // Mutate database: delete task and change setting
            taskRepository.hardDelete(task.getId());
            assertTrue(taskRepository.findById(task.getId()).isEmpty());
            viewModel.setPersonality(PersonalityType.STRICT);

            // Import backup
            viewModel.importBackupAsync(backupFile).join();

            // Verify task was restored
            var restoredTask = taskRepository.findById(task.getId());
            assertTrue(restoredTask.isPresent());
            assertEquals("Prepare Presentation Slides", restoredTask.get().getTitle());

            // Verify setting was restored
            assertEquals(PersonalityType.AGGRESSIVE, viewModel.personalityTypeProperty().get());
        } finally {
            backupFile.delete();
        }
    }
}
