package com.aarav.didyoudoit.service;

import javafx.stage.Stage;

import java.awt.TrayIcon;

/**
 * Service managing Windows system tray presence, context menu actions, and minimize-to-tray lifecycle.
 * Implements FR-18, FR-19, NFR-03, NFR-11, NFR-13.
 */
public interface TrayService {

    /**
     * Initializes the system tray icon, context menu, and hooks window close events.
     *
     * @param primaryStage application stage
     * @param taskService task management service
     * @param settingsService user settings service
     */
    void initialize(Stage primaryStage, TaskService taskService, SettingsService settingsService);

    /**
     * Restores and brings the main JavaFX application window to the foreground.
     */
    void restoreWindow();

    /**
     * Hides the main JavaFX application window to run quietly in the background.
     */
    void minimizeToTray();

    /**
     * Displays an informational notification message from the system tray icon.
     */
    void showTrayMessage(String title, String message);

    /**
     * Removes the tray icon upon application shutdown.
     */
    void shutdown();

    /**
     * Indicates whether the system tray is supported in the current environment.
     */
    boolean isSupported();

    /**
     * Returns the underlying AWT TrayIcon if available, or null.
     */
    TrayIcon getNativeTrayIcon();
}
