package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.service.PersonalityMessageService.NagMessage;

/**
 * Service interface for dispatching desktop and in-app task notifications.
 * Implements FR-04, FR-08, NFR-11, NFR-12.
 */
public interface NotificationService {

    /**
     * Listener callback invoked when user interacts with an action button on a notification.
     */
    interface NotificationActionListener {
        /**
         * Invoked when user clicks 'Mark Done'.
         */
        void onMarkDone(String taskId);

        /**
         * Invoked when user clicks 'Snooze' with the specified duration in minutes.
         */
        void onSnooze(String taskId, int minutes);

        /**
         * Invoked when user clicks 'Open App'.
         */
        void onOpenApp(String taskId);
    }

    /**
     * Registers a global action listener for notification interactions.
     *
     * @param listener callback listener
     */
    void setActionListener(NotificationActionListener listener);

    /**
     * Dispatches a notification for an overdue or due task.
     *
     * @param task target task
     * @param message personality-generated copy and escalation level
     */
    void notifyTask(Task task, NagMessage message);

    /**
     * Dismisses any active notification for a specific task.
     *
     * @param taskId target task ID
     */
    void dismissNotification(String taskId);
}
