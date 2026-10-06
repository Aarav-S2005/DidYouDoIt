package com.aarav.didyoudoit;

import com.aarav.didyoudoit.model.Priority;
import com.aarav.didyoudoit.model.RecurrenceRule;
import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.model.TaskCategory;
import com.aarav.didyoudoit.repository.*;
import com.aarav.didyoudoit.service.*;
import com.aarav.didyoudoit.ui.theme.Theme;
import com.aarav.didyoudoit.util.ClockService;
import com.aarav.didyoudoit.util.SystemClockService;
import com.aarav.didyoudoit.view.DashboardView;
import com.aarav.didyoudoit.viewmodel.DashboardViewModel;
import com.aarav.didyoudoit.viewmodel.SettingsViewModel;
import com.aarav.didyoudoit.viewmodel.StatisticsViewModel;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.logging.Logger;

/**
 * Main application entry point and manual composition root for DidYouDoIt.
 * Programs to interfaces with constructor injection; strictly avoids DI frameworks and FXML.
 * Implements FR-01, FR-02, FR-03, FR-12, FR-23.
 */
public class DidYouDoItApp extends Application {

    private static final Logger LOGGER = Logger.getLogger(DidYouDoItApp.class.getName());

    private DatabaseManager databaseManager;
    private NaggingDaemonService naggingDaemonService;
    private TrayService trayService;

    @Override
    public void start(Stage primaryStage) {
        // Enforce single instance: if another instance is running, activate it and exit immediately
        boolean isPrimaryInstance = com.aarav.didyoudoit.util.SingleInstanceManager.acquireInstanceLock(() -> {
            javafx.application.Platform.runLater(() -> {
                if (primaryStage != null) {
                    primaryStage.show();
                    primaryStage.setIconified(false);
                    primaryStage.toFront();
                    primaryStage.requestFocus();
                }
            });
        });

        if (!isPrimaryInstance) {
            LOGGER.info("Existing DidYouDoIt instance active. Exiting this duplicate process.");
            javafx.application.Platform.exit();
            System.exit(0);
            return;
        }

        LOGGER.info("Bootstrapping DidYouDoIt application...");

        // 1. Composition Root: Infrastructure & Repositories
        this.databaseManager = new SqliteDatabaseManager();
        TaskRepository taskRepository = new SqliteTaskRepository(databaseManager);
        HistoryRepository historyRepository = new SqliteHistoryRepository(databaseManager);
        SettingsRepository settingsRepository = new SqliteSettingsRepository(databaseManager);

        // 2. Services
        ClockService clockService = new SystemClockService();
        StreakService streakService = new StreakServiceImpl(historyRepository, clockService);
        RecurringTaskEngine recurringEngine = new RecurringTaskEngineImpl(taskRepository, clockService);
        PersonalityMessageService messageService = new PersonalityMessageServiceImpl();
        SettingsService settingsService = new SettingsServiceImpl(settingsRepository, clockService);
        DataBackupService backupService = new JsonDataBackupServiceImpl(taskRepository, settingsRepository, historyRepository);
        TaskService taskService = new TaskServiceImpl(
                taskRepository, historyRepository, streakService, recurringEngine, clockService
        );
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        StartupService startupService = isWindows ? new WindowsStartupServiceImpl() : new LinuxStartupServiceImpl();
        this.trayService = new WindowsTrayServiceImpl();

        // 3. ViewModels
        DashboardViewModel dashboardViewModel = new DashboardViewModel(taskService, recurringEngine, streakService);
        StatisticsViewModel statisticsViewModel = new StatisticsViewModel(taskService, streakService, historyRepository, clockService);
        SettingsViewModel settingsViewModel = new SettingsViewModel(settingsService, messageService, backupService, startupService);

        // 4. UI Assembly
        StackPane rootOverlay = new StackPane();
        DashboardView dashboardView = new DashboardView(dashboardViewModel, statisticsViewModel, settingsViewModel, rootOverlay);
        rootOverlay.getChildren().add(dashboardView);

        // 5. Notifications & Nagging Daemon Engine (FR-04, FR-05, FR-06, FR-07, FR-08)
        InAppNotificationService inAppNotificationService = new InAppNotificationService();
        inAppNotificationService.setToastConsumer(dashboardView::displayToast);

        NotificationService osNotificationService = isWindows
                ? new WindowsNativeNotificationService()
                : new LinuxNativeNotificationService();

        NotificationService compositeNotificationService = new CompositeNotificationService(
                inAppNotificationService,
                osNotificationService
        );

        this.naggingDaemonService = new NaggingDaemonServiceImpl(
                taskService,
                settingsService,
                messageService,
                compositeNotificationService,
                clockService
        );

        this.naggingDaemonService.setOnTaskUpdatedListener(() -> javafx.application.Platform.runLater(dashboardViewModel::reloadTasks));
        this.naggingDaemonService.setOnOpenAppRequestedListener(() -> javafx.application.Platform.runLater(() -> {
            primaryStage.show();
            primaryStage.toFront();
        }));
        this.naggingDaemonService.start();

        // 6. System Tray & Window Minimize-to-Tray (FR-18, FR-19)
        this.trayService.initialize(primaryStage, taskService, settingsService);
        if (this.trayService.getNativeTrayIcon() != null && osNotificationService instanceof WindowsNativeNotificationService winNotif) {
            winNotif.setSharedTrayIcon(this.trayService.getNativeTrayIcon());
        }

        Scene scene = new Scene(rootOverlay, 1100, 740);
        Theme.applyTheme(scene);

        primaryStage.setTitle("DidYouDoIt? - Personal Accountability");
        try (var iconStream = getClass().getResourceAsStream("/icons/app-icon.png")) {
            if (iconStream != null) {
                primaryStage.getIcons().add(new javafx.scene.image.Image(iconStream));
            }
        } catch (Exception ignored) {}
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(520);
        primaryStage.setMinHeight(540);
        primaryStage.setMaximized(true);
        primaryStage.show();
        primaryStage.setMaximized(true);

        LOGGER.info("DidYouDoIt Application is ready and running with background nagging daemon.");
    }



    @Override
    public void stop() {
        if (trayService != null) {
            trayService.shutdown();
        }
        if (naggingDaemonService != null) {
            naggingDaemonService.stop();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        com.aarav.didyoudoit.util.SingleInstanceManager.releaseInstanceLock();
    }

    public static void main(String[] args) {
        if (!com.aarav.didyoudoit.util.SingleInstanceManager.checkAndAcquireLockEarly()) {
            System.exit(0);
            return;
        }
        launch(args);
    }
}
