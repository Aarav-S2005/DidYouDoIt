package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.model.TaskStatus;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Native Windows implementation of {@link TrayService} managing the application tray icon,
 * minimize-to-tray lifecycle, and quick tray actions.
 * Implements FR-18, FR-19, NFR-03, NFR-13, NFR-14.
 */
public class WindowsTrayServiceImpl implements TrayService {

    private static final Logger LOGGER = Logger.getLogger(WindowsTrayServiceImpl.class.getName());

    private Stage primaryStage;
    private TaskService taskService;
    private SettingsService settingsService;
    private TrayIcon trayIcon;
    private boolean initialized = false;
    private boolean firstMinimizeNoticeShown = false;

    public WindowsTrayServiceImpl() {
    }

    @Override
    public synchronized void initialize(Stage stage, TaskService taskService, SettingsService settingsService) {
        this.primaryStage = stage;
        this.taskService = taskService;
        this.settingsService = settingsService;

        if (!SystemTray.isSupported()) {
            LOGGER.fine("SystemTray is not supported on this platform.");
            return;
        }

        try {
            Platform.setImplicitExit(false);

            // Hook window close request to minimize to tray
            if (primaryStage != null) {
                primaryStage.setOnCloseRequest(event -> {
                    event.consume();
                    minimizeToTray();
                });
            }

            Image iconImage = loadTrayIconImage();
            PopupMenu popup = createPopupMenu();

            this.trayIcon = new TrayIcon(iconImage, "DidYouDoIt? - Personal Accountability", popup);
            this.trayIcon.setImageAutoSize(true);

            // Double or single click on tray icon restores window
            this.trayIcon.addActionListener(e -> Platform.runLater(this::restoreWindow));

            SystemTray.getSystemTray().add(this.trayIcon);
            this.initialized = true;
            LOGGER.info("WindowsTrayServiceImpl successfully initialized tray icon.");
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Failed to initialize native Windows SystemTray: " + t.getMessage(), t);
        }
    }

    private PopupMenu createPopupMenu() {
        PopupMenu menu = new PopupMenu();

        MenuItem showItem = new MenuItem("Show DidYouDoIt");
        showItem.addActionListener(e -> Platform.runLater(this::restoreWindow));
        menu.add(showItem);

        menu.addSeparator();

        MenuItem summaryItem = new MenuItem("Today's Tasks Summary");
        summaryItem.addActionListener(e -> showTodaySummary());
        menu.add(summaryItem);

        MenuItem pauseItem = new MenuItem("Pause Reminders (1 Hour)");
        pauseItem.addActionListener(e -> {
            if (settingsService != null) {
                settingsService.pauseReminders(60);
                showTrayMessage("Reminders Paused", "Reminders are paused for the next 60 minutes.");
            }
        });
        menu.add(pauseItem);

        MenuItem resumeItem = new MenuItem("Resume Reminders");
        resumeItem.addActionListener(e -> {
            if (settingsService != null) {
                settingsService.resumeReminders();
                showTrayMessage("Reminders Resumed", "Accountability reminders are now active.");
            }
        });
        menu.add(resumeItem);

        menu.addSeparator();

        MenuItem exitItem = new MenuItem("Exit DidYouDoIt");
        exitItem.addActionListener(e -> {
            shutdown();
            Platform.runLater(() -> {
                if (primaryStage != null) {
                    primaryStage.close();
                }
                Platform.exit();
                System.exit(0);
            });
        });
        menu.add(exitItem);

        return menu;
    }

    private void showTodaySummary() {
        if (taskService == null) {
            return;
        }

        try {
            List<Task> todayTasks = taskService.getTodayTasks();
            long pending = todayTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.PENDING || t.getStatus() == TaskStatus.POSTPONED)
                    .count();
            long overdue = taskService.getOverdueTasks().size();
            long completed = todayTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                    .count();

            String title = "Today's Status";
            String message = String.format("Tasks: %d pending, %d overdue, %d completed.", pending, overdue, completed);
            showTrayMessage(title, message);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error showing today summary: " + ex.getMessage(), ex);
        }
    }

    private Image loadTrayIconImage() {
        try (var stream = getClass().getResourceAsStream("/icons/app-icon-32.png")) {
            if (stream != null) {
                return javax.imageio.ImageIO.read(stream);
            }
        } catch (Exception ignored) {
        }
        return createTrayIconImage();
    }

    private Image createTrayIconImage() {
        int size = 32;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Warm Coral circular background
        g.setColor(new Color(226, 125, 96));
        g.fillRoundRect(2, 2, size - 4, size - 4, 10, 10);

        // Crisp white checkmark
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int[] xPoints = {9, 14, 23};
        int[] yPoints = {17, 22, 11};
        g.drawPolyline(xPoints, yPoints, 3);

        g.dispose();
        return image;
    }

    @Override
    public void restoreWindow() {
        if (primaryStage != null) {
            primaryStage.show();
            primaryStage.setIconified(false);
            primaryStage.toFront();
        }
    }

    @Override
    public void minimizeToTray() {
        if (primaryStage != null) {
            primaryStage.hide();
            if (!firstMinimizeNoticeShown && initialized && trayIcon != null) {
                firstMinimizeNoticeShown = true;
                showTrayMessage("DidYouDoIt Running in Background",
                        "The app is minimized to the system tray to keep reminding you about your tasks.");
            }
        }
    }

    @Override
    public void showTrayMessage(String title, String message) {
        if (initialized && trayIcon != null) {
            try {
                trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
            } catch (Throwable t) {
                LOGGER.log(Level.FINE, "Failed to display tray message: " + t.getMessage());
            }
        }
    }

    @Override
    public synchronized void shutdown() {
        if (initialized && trayIcon != null) {
            try {
                SystemTray.getSystemTray().remove(trayIcon);
            } catch (Throwable ignored) {
            }
            initialized = false;
            trayIcon = null;
        }
    }

    @Override
    public boolean isSupported() {
        return initialized && trayIcon != null;
    }

    @Override
    public TrayIcon getNativeTrayIcon() {
        return trayIcon;
    }
}
