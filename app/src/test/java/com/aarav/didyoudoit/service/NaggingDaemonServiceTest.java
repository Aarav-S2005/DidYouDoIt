package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.*;
import com.aarav.didyoudoit.repository.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class NaggingDaemonServiceTest {

    private Path tempDbPath;
    private DatabaseManager dbManager;
    private TaskRepository taskRepository;
    private HistoryRepository historyRepository;
    private SettingsRepository settingsRepository;
    private TestClockService clockService;
    private TaskService taskService;
    private SettingsService settingsService;
    private PersonalityMessageService messageService;
    private TestNotificationService notificationService;
    private NaggingDaemonServiceImpl daemonService;

    private static class TestNotificationService implements NotificationService {
        final List<Task> notifiedTasks = new ArrayList<>();
        final List<PersonalityMessageService.NagMessage> messages = new ArrayList<>();
        final List<String> dismissedTaskIds = new ArrayList<>();
        NotificationActionListener listener;

        @Override
        public void setActionListener(NotificationActionListener listener) {
            this.listener = listener;
        }

        @Override
        public void notifyTask(Task task, PersonalityMessageService.NagMessage message) {
            notifiedTasks.add(task);
            messages.add(message);
        }

        @Override
        public void dismissNotification(String taskId) {
            dismissedTaskIds.add(taskId);
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        tempDbPath = Files.createTempFile("didyoudoit_daemon_test_", ".db");
        dbManager = new SqliteDatabaseManager(tempDbPath.toString());
        taskRepository = new SqliteTaskRepository(dbManager);
        historyRepository = new SqliteHistoryRepository(dbManager);
        settingsRepository = new SqliteSettingsRepository(dbManager);

        clockService = new TestClockService(LocalDateTime.of(2026, 10, 6, 9, 0));
        StreakService streakService = new StreakServiceImpl(historyRepository, clockService);
        RecurringTaskEngine recurringEngine = new RecurringTaskEngineImpl(taskRepository, clockService);
        taskService = new TaskServiceImpl(taskRepository, historyRepository, streakService, recurringEngine, clockService);
        settingsService = new SettingsServiceImpl(settingsRepository, clockService);
        messageService = new PersonalityMessageServiceImpl();
        notificationService = new TestNotificationService();

        daemonService = new NaggingDaemonServiceImpl(
                taskService,
                settingsService,
                messageService,
                notificationService,
                clockService,
                1 // 1 second interval for testing
        );
    }

    @AfterEach
    void tearDown() {
        if (daemonService.isRunning()) {
            daemonService.stop();
        }
        dbManager.close();
        try {
            Files.deleteIfExists(tempDbPath);
        } catch (IOException ignored) {}
    }

    @Test
    @DisplayName("Daemon starts and stops cleanly")
    void testStartAndStop() {
        assertFalse(daemonService.isRunning());
        daemonService.start();
        assertTrue(daemonService.isRunning());
        daemonService.stop();
        assertFalse(daemonService.isRunning());
    }

    @Test
    @DisplayName("Overdue task triggers nagging notification and increments nag count")
    void testOverdueTaskTriggersNag() {
        // Task due at 8:30 (current clock is 9:00 -> 30 mins overdue)
        Task overdueTask = taskService.createTask(Task.builder()
                .title("Submit Tax Documents")
                .dueDateTime(LocalDateTime.of(2026, 10, 6, 8, 30))
                .priority(Priority.HIGH)
                .category(TaskCategory.WORK)
                .build());

        daemonService.checkAndNagNow();

        assertEquals(1, notificationService.notifiedTasks.size(), "Should dispatch 1 notification");
        assertEquals("Submit Tax Documents", notificationService.notifiedTasks.get(0).getTitle());
        assertNotNull(notificationService.messages.get(0));

        Task refreshed = taskService.getTask(overdueTask.getId()).orElseThrow();
        assertEquals(1, refreshed.getNagCount(), "Nag count should have incremented to 1");
    }

    @Test
    @DisplayName("Quiet hours suppress nagging notifications")
    void testQuietHoursSuppression() {
        // Configure quiet hours from 08:00 to 10:00 (clock is 09:00)
        AppSettings settings = settingsService.getSettings();
        settings.setQuietHoursEnabled(true);
        settings.setQuietHoursStart(LocalTime.of(8, 0));
        settings.setQuietHoursEnd(LocalTime.of(10, 0));
        settingsService.updateSettings(settings);

        taskService.createTask(Task.builder()
                .title("Overdue Task During Quiet Hours")
                .dueDateTime(LocalDateTime.of(2026, 10, 6, 8, 30))
                .priority(Priority.MEDIUM)
                .category(TaskCategory.STUDY)
                .build());

        daemonService.checkAndNagNow();

        assertTrue(notificationService.notifiedTasks.isEmpty(), "No notifications during quiet hours");
    }

    @Test
    @DisplayName("Reminders pause setting suppresses nagging")
    void testRemindersPausedSuppression() {
        taskService.createTask(Task.builder()
                .title("Overdue Task During Pause")
                .dueDateTime(LocalDateTime.of(2026, 10, 6, 8, 30))
                .priority(Priority.MEDIUM)
                .category(TaskCategory.STUDY)
                .build());

        settingsService.pauseReminders(60);

        daemonService.checkAndNagNow();

        assertTrue(notificationService.notifiedTasks.isEmpty(), "No notifications when reminders are paused");
    }

    @Test
    @DisplayName("Escalation interval prevents spamming until interval elapses")
    void testEscalationIntervalEnforced() {
        AppSettings settings = settingsService.getSettings();
        settings.setEscalationIntervalMinutes(10);
        settingsService.updateSettings(settings);

        Task task = taskService.createTask(Task.builder()
                .title("Review Pull Request")
                .dueDateTime(LocalDateTime.of(2026, 10, 6, 8, 0))
                .priority(Priority.MEDIUM)
                .category(TaskCategory.WORK)
                .build());

        // First pass -> nag dispatched
        daemonService.checkAndNagNow();
        assertEquals(1, notificationService.notifiedTasks.size());

        // Advance clock by 3 minutes (less than 10 mins)
        clockService.advanceMinutes(3);
        daemonService.checkAndNagNow();
        assertEquals(1, notificationService.notifiedTasks.size(), "Should NOT nag before interval elapsed");

        // Advance clock past the 10 min interval (total +11 minutes)
        clockService.advanceMinutes(8);
        daemonService.checkAndNagNow();
        assertEquals(2, notificationService.notifiedTasks.size(), "Should dispatch second escalated nag");

        Task refreshed = taskService.getTask(task.getId()).orElseThrow();
        assertEquals(2, refreshed.getNagCount());
    }

    @Test
    @DisplayName("Action callback Mark Done completes task and dismisses notification")
    void testActionCallbackMarkDone() {
        Task task = taskService.createTask(Task.builder()
                .title("Clean Desk")
                .dueDateTime(LocalDateTime.of(2026, 10, 6, 8, 0))
                .priority(Priority.LOW)
                .category(TaskCategory.PERSONAL)
                .build());

        AtomicBoolean updatedCalled = new AtomicBoolean(false);
        daemonService.setOnTaskUpdatedListener(() -> updatedCalled.set(true));

        daemonService.onMarkDone(task.getId());

        Task updated = taskService.getTask(task.getId()).orElseThrow();
        assertEquals(TaskStatus.COMPLETED, updated.getStatus());
        assertTrue(notificationService.dismissedTaskIds.contains(task.getId()));
        assertTrue(updatedCalled.get());
    }

    @Test
    @DisplayName("Action callback Snooze postpones task and dismisses notification")
    void testActionCallbackSnooze() {
        Task task = taskService.createTask(Task.builder()
                .title("Call Dentist")
                .dueDateTime(LocalDateTime.of(2026, 10, 6, 8, 0))
                .priority(Priority.MEDIUM)
                .category(TaskCategory.HEALTH)
                .build());

        AtomicBoolean updatedCalled = new AtomicBoolean(false);
        daemonService.setOnTaskUpdatedListener(() -> updatedCalled.set(true));

        daemonService.onSnooze(task.getId(), 20);

        Task updated = taskService.getTask(task.getId()).orElseThrow();
        assertEquals(TaskStatus.POSTPONED, updated.getStatus());
        assertNotNull(updated.getPostponedUntil());
        assertEquals(LocalDateTime.of(2026, 10, 6, 9, 20), updated.getPostponedUntil());
        assertTrue(notificationService.dismissedTaskIds.contains(task.getId()));
        assertTrue(updatedCalled.get());
    }
}
