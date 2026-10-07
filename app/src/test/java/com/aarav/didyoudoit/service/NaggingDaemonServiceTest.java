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
    @DisplayName("Due task triggers initial notification, then subsequent pass after interval triggers nag")
    void testOverdueTaskTriggersNag() {
        // Task due at 8:30 (current clock is 9:00 -> 30 mins overdue)
        Task overdueTask = taskService.createTask(Task.builder()
                .title("Submit Tax Documents")
                .dueDateTime(LocalDateTime.of(2026, 10, 6, 8, 30))
                .priority(Priority.HIGH)
                .category(TaskCategory.WORK)
                .build());

        // First pass: on-time due notification sent (not a nag, nag count = 0)
        daemonService.checkAndNagNow();

        assertEquals(1, notificationService.notifiedTasks.size(), "Should dispatch initial due notification");
        assertEquals("Submit Tax Documents", notificationService.notifiedTasks.get(0).getTitle());
        assertNotNull(notificationService.messages.get(0));

        Task initial = taskService.getTask(overdueTask.getId()).orElseThrow();
        assertEquals(0, initial.getNagCount(), "Initial due notification must NOT increment nag count");

        // Advance clock past the escalation interval (10 mins default)
        clockService.advanceMinutes(11);
        daemonService.checkAndNagNow();

        assertEquals(2, notificationService.notifiedTasks.size(), "Should dispatch first nag");
        Task refreshed = taskService.getTask(overdueTask.getId()).orElseThrow();
        assertEquals(1, refreshed.getNagCount(), "First nag should increment nag count to 1");
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

        // First pass -> initial due notification dispatched (nag count = 0)
        daemonService.checkAndNagNow();
        assertEquals(1, notificationService.notifiedTasks.size());
        assertEquals(0, taskService.getTask(task.getId()).orElseThrow().getNagCount());

        // Advance clock by 3 minutes (less than 10 mins)
        clockService.advanceMinutes(3);
        daemonService.checkAndNagNow();
        assertEquals(1, notificationService.notifiedTasks.size(), "Should NOT nag before interval elapsed");

        // Advance clock past the 10 min interval (total +11 minutes from initial)
        clockService.advanceMinutes(8);
        daemonService.checkAndNagNow();
        assertEquals(2, notificationService.notifiedTasks.size(), "Should dispatch first escalated nag");

        Task refreshed = taskService.getTask(task.getId()).orElseThrow();
        assertEquals(1, refreshed.getNagCount());

        // Advance clock past another 10 min interval
        clockService.advanceMinutes(11);
        daemonService.checkAndNagNow();
        assertEquals(3, notificationService.notifiedTasks.size(), "Should dispatch second escalated nag");
        assertEquals(2, taskService.getTask(task.getId()).orElseThrow().getNagCount());
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

    @Test
    @DisplayName("Do not nag when timer is on for that task")
    void testTimerActiveSuppressesNagging() {
        // Task due at 8:30 (current clock is 9:00 -> 30 mins overdue)
        Task task = taskService.createTask(Task.builder()
                .title("Focus Coding Session")
                .dueDateTime(LocalDateTime.of(2026, 10, 6, 8, 30))
                .priority(Priority.HIGH)
                .category(TaskCategory.WORK)
                .durationMinutes(25)
                .timerRemainingSeconds(1500)
                .timerActive(true)
                .build());

        // Daemon runs: timer is active, so nagging and due notifications must be suppressed
        daemonService.checkAndNagNow();

        assertTrue(notificationService.notifiedTasks.isEmpty(), "Nagging must be suppressed when timer is active");
        Task refreshed = taskService.getTask(task.getId()).orElseThrow();
        assertEquals(0, refreshed.getNagCount(), "Nag count must remain 0 while timer is running");

        // When timer is stopped/paused (timerActive = false), nagging can proceed
        refreshed.setTimerActive(false);
        taskService.updateTask(refreshed);

        daemonService.checkAndNagNow();
        assertEquals(1, notificationService.notifiedTasks.size(), "Due notification dispatched once timer is no longer active");
    }

    @Test
    @DisplayName("Do not allow task to be marked done via notification if timer has remaining time")
    void testActionCallbackMarkDoneBlockedWhenTimerHasRemainingSeconds() {
        Task task = taskService.createTask(Task.builder()
                .title("Study Biology")
                .dueDateTime(LocalDateTime.of(2026, 10, 6, 8, 0))
                .durationMinutes(30)
                .timerRemainingSeconds(1800)
                .timerActive(false)
                .build());

        // Attempt mark done while paused with 30m remaining
        daemonService.onMarkDone(task.getId());

        Task notCompleted = taskService.getTask(task.getId()).orElseThrow();
        assertNotEquals(TaskStatus.COMPLETED, notCompleted.getStatus(), "Task must not be marked done when timer is not zero");

        // Now set timer remaining to zero
        notCompleted.setTimerRemainingSeconds(0);
        taskService.updateTask(notCompleted);

        daemonService.onMarkDone(task.getId());

        Task completed = taskService.getTask(task.getId()).orElseThrow();
        assertEquals(TaskStatus.COMPLETED, completed.getStatus(), "Task can be marked done once timer reaches zero");
    }
}
