package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.EscalationLevel;
import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.service.PersonalityMessageService.NagMessage;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Linux desktop notification service utilizing the Freedesktop.org desktop notification standard
 * via {@code notify-send}.
 * Compatible across all Linux desktop distributions (Ubuntu, Fedora, Arch, Debian, Mint, openSUSE).
 * Implements FR-04, FR-08, NFR-12, NFR-13, NFR-14.
 */
public class LinuxNativeNotificationService implements NotificationService {

    private static final Logger LOGGER = Logger.getLogger(LinuxNativeNotificationService.class.getName());

    private NotificationActionListener actionListener;

    public LinuxNativeNotificationService() {
    }

    @Override
    public void setActionListener(NotificationActionListener listener) {
        this.actionListener = listener;
    }

    @Override
    public void notifyTask(Task task, NagMessage message) {
        if (task == null || message == null) {
            return;
        }

        try {
            String urgency = switch (message.level()) {
                case INITIAL -> "low";
                case NUDGE, WARN -> "normal";
                case CRITICAL -> "critical";
            };

            String headline = message.title();
            String body = message.body();

            ProcessBuilder pb = new ProcessBuilder(
                    "notify-send",
                    "-a", "DidYouDoIt",
                    "-u", urgency,
                    "-i", "dialog-information",
                    headline,
                    body
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();
            LOGGER.fine("Dispatched Linux notify-send notification for: " + task.getTitle());
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "Failed to invoke notify-send on Linux (may be headless or missing utility): " + t.getMessage());
        }
    }

    @Override
    public void dismissNotification(String taskId) {
        // notify-send notifications are managed transiently by the Freedesktop notification daemon
    }
}
