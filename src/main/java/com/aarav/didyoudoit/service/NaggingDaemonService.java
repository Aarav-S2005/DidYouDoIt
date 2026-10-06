package com.aarav.didyoudoit.service;

/**
 * Background engine that monitors task due dates and overdue status,
 * coordinates escalation levels, and schedules reminders/nags.
 * Implements FR-04, FR-05, FR-06, FR-07, FR-08, FR-17, FR-25, NFR-01, NFR-03, NFR-15.
 */
public interface NaggingDaemonService {

    /**
     * Starts the background monitoring and nagging scheduler.
     */
    void start();

    /**
     * Gracefully stops the background daemon executor.
     */
    void stop();

    /**
     * Checks if the daemon scheduler is currently active.
     */
    boolean isRunning();

    /**
     * Executes an immediate evaluation pass for overdue tasks and dispatches nags if due.
     * Useful for manual triggers and deterministic testing.
     */
    void checkAndNagNow();

    /**
     * Registers a listener notified when a task is completed or snoozed via notification actions.
     */
    void setOnTaskUpdatedListener(Runnable listener);

    /**
     * Registers a listener notified when user clicks 'Open App' on a notification.
     */
    void setOnOpenAppRequestedListener(Runnable listener);
}
