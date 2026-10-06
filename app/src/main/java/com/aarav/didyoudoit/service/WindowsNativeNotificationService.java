package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.EscalationLevel;
import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.service.PersonalityMessageService.NagMessage;

import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Native Windows 10/11 notification service utilizing Windows System Tray / Action Center
 * notifications and WinRT integration.
 * Implements FR-04, FR-08, NFR-13, NFR-14.
 */
public class WindowsNativeNotificationService implements NotificationService {

    private static final Logger LOGGER = Logger.getLogger(WindowsNativeNotificationService.class.getName());

    private NotificationActionListener actionListener;
    private TrayIcon trayIcon;
    private boolean initialized = false;

    public WindowsNativeNotificationService() {
        initTrayIcon();
    }

    private synchronized void initTrayIcon() {
        try {
            if (SystemTray.isSupported()) {
                SystemTray tray = SystemTray.getSystemTray();
                // Check if already has an icon from tray service
                TrayIcon[] existing = tray.getTrayIcons();
                if (existing != null && existing.length > 0) {
                    this.trayIcon = existing[0];
                    this.initialized = true;
                    return;
                }

                Image image = Toolkit.getDefaultToolkit().createImage(new byte[0]);
                this.trayIcon = new TrayIcon(image, "DidYouDoIt?");
                this.trayIcon.setImageAutoSize(true);
                tray.add(this.trayIcon);
                this.initialized = true;
                LOGGER.info("WindowsNativeNotificationService initialized native SystemTray.");
            } else {
                LOGGER.fine("SystemTray not supported in this environment; native notifications disabled.");
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Unable to initialize native SystemTray for notifications: " + t.getMessage());
        }
    }

    /**
     * Attaches an existing SystemTray TrayIcon instance (e.g., from WindowsTrayService in Phase 9).
     */
    public synchronized void setSharedTrayIcon(TrayIcon icon) {
        this.trayIcon = icon;
        this.initialized = (icon != null);
    }

    @Override
    public void setActionListener(NotificationActionListener listener) {
        this.actionListener = listener;
        if (trayIcon != null && listener != null) {
            trayIcon.addActionListener(e -> listener.onOpenApp(null));
        }
    }

    @Override
    public void notifyTask(Task task, NagMessage message) {
        if (task == null || message == null) {
            return;
        }

        try {
            if (initialized && trayIcon != null) {
                TrayIcon.MessageType msgType = switch (message.level()) {
                    case INITIAL, NUDGE -> TrayIcon.MessageType.INFO;
                    case WARN -> TrayIcon.MessageType.WARNING;
                    case CRITICAL -> TrayIcon.MessageType.ERROR;
                };

                String headline = message.title();
                String body = task.getTitle() + "\n" + message.body();
                trayIcon.displayMessage(headline, body, msgType);
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Failed to display native Windows notification: " + t.getMessage());
        }
    }

    @Override
    public void dismissNotification(String taskId) {
        // Native Windows TrayIcon notifications are transiently managed by the OS notification center
    }

    public boolean isSupported() {
        return initialized && trayIcon != null;
    }
}
