package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.AppSettings;
import com.aarav.didyoudoit.model.EscalationLevel;
import com.aarav.didyoudoit.model.Priority;
import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.service.PersonalityMessageService.NagMessage;
import com.aarav.didyoudoit.util.ClockService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Concrete implementation of {@link NaggingDaemonService} that periodically evaluates
 * active tasks against deadlines, handles escalating reminders, respects quiet hours,
 * and processes notification action callbacks.
 * Implements FR-04, FR-05, FR-06, FR-07, FR-08, FR-17, FR-25, NFR-01, NFR-03, NFR-15.
 */
public class NaggingDaemonServiceImpl implements NaggingDaemonService, NotificationService.NotificationActionListener {

    private static final Logger LOGGER = Logger.getLogger(NaggingDaemonServiceImpl.class.getName());
    private static final long DEFAULT_POLL_INTERVAL_SECONDS = 15;

    private final TaskService taskService;
    private final SettingsService settingsService;
    private final PersonalityMessageService personalityMessageService;
    private final NotificationService notificationService;
    private final ClockService clockService;
    private final long pollIntervalSeconds;

    private final Map<String, LocalDateTime> lastNaggedTimes = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;
    private Runnable onTaskUpdatedListener;
    private Runnable onOpenAppRequestedListener;

    public NaggingDaemonServiceImpl(
            TaskService taskService,
            SettingsService settingsService,
            PersonalityMessageService personalityMessageService,
            NotificationService notificationService,
            ClockService clockService) {
        this(taskService, settingsService, personalityMessageService, notificationService, clockService, DEFAULT_POLL_INTERVAL_SECONDS);
    }

    public NaggingDaemonServiceImpl(
            TaskService taskService,
            SettingsService settingsService,
            PersonalityMessageService personalityMessageService,
            NotificationService notificationService,
            ClockService clockService,
            long pollIntervalSeconds) {
        this.taskService = Objects.requireNonNull(taskService, "taskService must not be null");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService must not be null");
        this.personalityMessageService = Objects.requireNonNull(personalityMessageService, "personalityMessageService must not be null");
        this.notificationService = Objects.requireNonNull(notificationService, "notificationService must not be null");
        this.clockService = Objects.requireNonNull(clockService, "clockService must not be null");
        this.pollIntervalSeconds = Math.max(1, pollIntervalSeconds);

        this.notificationService.setActionListener(this);
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "NaggingDaemon-Worker");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleWithFixedDelay(
                this::safeCheckAndNag,
                2,
                pollIntervalSeconds,
                TimeUnit.SECONDS
        );

        running = true;
        LOGGER.info("NaggingDaemonService started with " + pollIntervalSeconds + "s interval.");
    }

    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }

        running = false;
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        LOGGER.info("NaggingDaemonService stopped.");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void setOnTaskUpdatedListener(Runnable listener) {
        this.onTaskUpdatedListener = listener;
    }

    @Override
    public void setOnOpenAppRequestedListener(Runnable listener) {
        this.onOpenAppRequestedListener = listener;
    }

    private void safeCheckAndNag() {
        try {
            checkAndNagNow();
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Error during nagging daemon pass: " + t.getMessage(), t);
        }
    }

    @Override
    public void checkAndNagNow() {
        // 1. Check if reminders are currently suppressed (quiet hours or manual pause)
        if (settingsService.areRemindersSuppressed()) {
            LOGGER.fine("Reminders currently suppressed by quiet hours or pause setting.");
            return;
        }

        AppSettings settings = settingsService.getSettings();
        LocalDateTime now = clockService.now();

        // 2. Query overdue and due tasks
        List<Task> overdueTasks = taskService.getOverdueTasks();
        if (overdueTasks.isEmpty()) {
            return;
        }

        for (Task task : overdueTasks) {
            try {
                processTaskNag(task, settings, now);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to process nag for task " + task.getId(), e);
            }
        }
    }

    private void processTaskNag(Task task, AppSettings settings, LocalDateTime now) {
        // Do not nag when timer is on for that task
        if (task.isTimerActive()) {
            LOGGER.fine("Suppressing nag: focus timer is actively running for '" + task.getTitle() + "'");
            notificationService.dismissNotification(task.getId());
            return;
        }

        LocalDateTime effectiveDeadline = task.getPostponedUntil() != null
                ? task.getPostponedUntil()
                : task.getDueDateTime();

        if (effectiveDeadline == null || now.isBefore(effectiveDeadline)) {
            return;
        }

        LocalDateTime lastNag = lastNaggedTimes.get(task.getId());
        boolean isFirstNotificationForCycle = (lastNag == null || lastNag.isBefore(effectiveDeadline));

        if (isFirstNotificationForCycle) {
            // Initial Due Notification (FR-04): Task has just reached its deadline.
            // Dispatch a friendly on-time notification without nagging or advancing the escalation level.
            NagMessage dueMessage = new NagMessage(
                    "Task Due: " + task.getTitle(),
                    "It's time for '" + task.getTitle() + "'!",
                    EscalationLevel.INITIAL
            );

            notificationService.notifyTask(task, dueMessage);
            lastNaggedTimes.put(task.getId(), now);
            LOGGER.info("Dispatched initial on-time due notification for '" + task.getTitle() + "'");
            return;
        }

        // Check if persistent nagging is disabled and task was already nagged once
        if (!settings.isPersistentNaggingEnabled() && task.getNagCount() > 0) {
            return;
        }

        // Recurring nag evaluation (FR-05, FR-06)
        // Respect the user's explicit interval setting with exact second-level precision.
        long secondsSinceLastNag = Duration.between(lastNag, now).getSeconds();
        int baseIntervalMinutes = Math.max(1, settings.getEscalationIntervalMinutes());
        long requiredSeconds = baseIntervalMinutes * 60L;

        if (secondsSinceLastNag >= requiredSeconds) {
            // The full nagging interval has elapsed. Advance nag count and escalation in database.
            taskService.recordNag(task.getId());
            lastNaggedTimes.put(task.getId(), now);

            // Fetch fresh state of task to ensure updated escalation level
            Task refreshedTask = taskService.getTask(task.getId()).orElse(task);

            long minutesOverdue = Math.max(0, Duration.between(effectiveDeadline, now).toMinutes());
            NagMessage message = personalityMessageService.generateMessage(
                    refreshedTask,
                    settings.getPersonalityType(),
                    (int) minutesOverdue
            );

            notificationService.notifyTask(refreshedTask, message);
            LOGGER.info("Dispatched nag #" + refreshedTask.getNagCount() + " for '" + refreshedTask.getTitle()
                    + "' [Level: " + refreshedTask.getEscalationLevel() + ", Overdue: " + minutesOverdue + "m]");
        }
    }

    // -------------------------------------------------------------------------
    // NotificationActionListener Implementation
    // -------------------------------------------------------------------------

    @Override
    public void onMarkDone(String taskId) {
        if (taskId == null) return;
        LOGGER.info("Notification action Mark Done received for task: " + taskId);

        var taskOpt = taskService.getTask(taskId);
        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            if (task.hasDuration() && task.getTimerRemainingSeconds() > 0) {
                LOGGER.warning("Cannot mark task '" + task.getTitle() + "' as done: focus timer still has "
                        + task.getTimerRemainingSeconds() + "s remaining.");
                return;
            }
        }

        try {
            taskService.completeTask(taskId);
            notificationService.dismissNotification(taskId);
            lastNaggedTimes.remove(taskId);

            if (onTaskUpdatedListener != null) {
                onTaskUpdatedListener.run();
            }
        } catch (IllegalStateException e) {
            LOGGER.warning("Could not complete task " + taskId + ": " + e.getMessage());
        }
    }

    @Override
    public void onSnooze(String taskId, int minutes) {
        if (taskId == null) return;
        AppSettings settings = settingsService.getSettings();
        int snoozeDuration = minutes > 0 ? minutes : settings.getDefaultSnoozeMinutes();

        LOGGER.info("Notification action Snooze received for task " + taskId + " (" + snoozeDuration + "m)");
        taskService.postponeTask(taskId, snoozeDuration);
        notificationService.dismissNotification(taskId);
        lastNaggedTimes.remove(taskId);

        if (onTaskUpdatedListener != null) {
            onTaskUpdatedListener.run();
        }
    }

    @Override
    public void onOpenApp(String taskId) {
        LOGGER.info("Notification action Open App received for task: " + taskId);
        if (onOpenAppRequestedListener != null) {
            onOpenAppRequestedListener.run();
        }
    }
}
