package com.aarav.didyoudoit.viewmodel;

import com.aarav.didyoudoit.model.AppSettings;
import com.aarav.didyoudoit.model.PersonalityType;
import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.service.DataBackupService;
import com.aarav.didyoudoit.service.PersonalityMessageService;
import com.aarav.didyoudoit.service.SettingsService;
import com.aarav.didyoudoit.service.StartupService;
import javafx.application.Platform;
import javafx.beans.property.*;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Presentation ViewModel for application settings, personality customization,
 * quiet hours, nagging intervals, and backup export/import.
 * Implements FR-15, FR-16, FR-17, FR-20, FR-24, FR-25.
 */
public class SettingsViewModel {

    private final SettingsService settingsService;
    private final PersonalityMessageService personalityMessageService;
    private final DataBackupService dataBackupService;

    // Observable presentation properties
    private final ObjectProperty<PersonalityType> personalityType = new SimpleObjectProperty<>(PersonalityType.SARCASTIC);
    private final BooleanProperty quietHoursEnabled = new SimpleBooleanProperty(false);
    private final ObjectProperty<LocalTime> quietHoursStart = new SimpleObjectProperty<>(LocalTime.of(22, 0));
    private final ObjectProperty<LocalTime> quietHoursEnd = new SimpleObjectProperty<>(LocalTime.of(8, 0));
    private final BooleanProperty remindersPaused = new SimpleBooleanProperty(false);
    private final BooleanProperty autoStartOnBoot = new SimpleBooleanProperty(false);
    private final BooleanProperty persistentNaggingEnabled = new SimpleBooleanProperty(true);
    private final IntegerProperty escalationIntervalMinutes = new SimpleIntegerProperty(10);
    private final IntegerProperty defaultSnoozeMinutes = new SimpleIntegerProperty(15);
    private final StringProperty previewSampleQuote = new SimpleStringProperty("");
    private final StringProperty statusMessage = new SimpleStringProperty("");

    private final StartupService startupService;
    private final com.aarav.didyoudoit.service.UpdateService updateService;

    // In-app update properties
    private final StringProperty appVersion = new SimpleStringProperty(com.aarav.didyoudoit.util.AppVersion.get());
    private final BooleanProperty checkingForUpdate = new SimpleBooleanProperty(false);
    private final BooleanProperty updateAvailable = new SimpleBooleanProperty(false);
    private final StringProperty latestVersionString = new SimpleStringProperty("v1.1.4");
    private final StringProperty updateDownloadUrl = new SimpleStringProperty("");
    private final StringProperty updateMsiUrl = new SimpleStringProperty("");
    private final StringProperty updateZipUrl = new SimpleStringProperty("");
    private final StringProperty updateDebUrl = new SimpleStringProperty("");
    private final StringProperty updateTarGzUrl = new SimpleStringProperty("");
    private final StringProperty updateReleaseUrl = new SimpleStringProperty("");
    private final StringProperty updateStatusMessage = new SimpleStringProperty("You are on the latest version.");
    private final BooleanProperty downloadingUpdate = new SimpleBooleanProperty(false);
    private final DoubleProperty downloadProgress = new SimpleDoubleProperty(0.0);

    public SettingsViewModel(SettingsService settingsService,
                             PersonalityMessageService personalityMessageService,
                             DataBackupService dataBackupService) {
        this(settingsService, personalityMessageService, dataBackupService, null, new com.aarav.didyoudoit.service.UpdateServiceImpl());
    }

    public SettingsViewModel(SettingsService settingsService,
                             PersonalityMessageService personalityMessageService,
                             DataBackupService dataBackupService,
                             StartupService startupService) {
        this(settingsService, personalityMessageService, dataBackupService, startupService, new com.aarav.didyoudoit.service.UpdateServiceImpl());
    }

    public SettingsViewModel(SettingsService settingsService,
                             PersonalityMessageService personalityMessageService,
                             DataBackupService dataBackupService,
                             StartupService startupService,
                             com.aarav.didyoudoit.service.UpdateService updateService) {
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService cannot be null");
        this.personalityMessageService = Objects.requireNonNull(personalityMessageService, "personalityMessageService cannot be null");
        this.dataBackupService = Objects.requireNonNull(dataBackupService, "dataBackupService cannot be null");
        this.startupService = startupService;
        this.updateService = Objects.requireNonNull(updateService, "updateService cannot be null");
        this.appVersion.set(updateService.getCurrentVersion());

        loadSettings();
    }

    /**
     * Loads saved settings from repository into observable properties.
     */
    public void loadSettings() {
        AppSettings settings = settingsService.getSettings();
        personalityType.set(settings.getPersonalityType());
        quietHoursEnabled.set(settings.isQuietHoursEnabled());
        quietHoursStart.set(settings.getQuietHoursStart());
        quietHoursEnd.set(settings.getQuietHoursEnd());
        remindersPaused.set(settingsService.areRemindersSuppressed());
        autoStartOnBoot.set(settings.isAutoStartOnBoot());
        persistentNaggingEnabled.set(settings.isPersistentNaggingEnabled());
        escalationIntervalMinutes.set(settings.getEscalationIntervalMinutes());
        defaultSnoozeMinutes.set(settings.getDefaultSnoozeMinutes());

        updatePreviewQuote(settings.getPersonalityType());
    }

    /**
     * Updates personality mode and generates live sample preview quotes (FR-15, FR-16).
     */
    public void setPersonality(PersonalityType personality) {
        if (personality == null) return;
        personalityType.set(personality);
        settingsService.setPersonality(personality);
        updatePreviewQuote(personality);
    }

    /**
     * Updates preview sample quotes for the selected personality.
     */
    public void updatePreviewQuote(PersonalityType personality) {
        if (personality == null) return;
        Task initialTask = Task.builder().title("Finish Project Report").nagCount(0).build();
        Task nudgeTask = Task.builder().title("Finish Project Report").nagCount(2).build();
        Task criticalTask = Task.builder().title("Finish Project Report").nagCount(7).build();

        var initial = personalityMessageService.generateMessage(initialTask, personality, 0);
        var nudge = personalityMessageService.generateMessage(nudgeTask, personality, 10);
        var critical = personalityMessageService.generateMessage(criticalTask, personality, 60);

        String preview = String.format(
                "Initial: \"%s - %s\"\nNudge: \"%s - %s\"\nUrgent: \"%s - %s\"",
                initial.title(), initial.body(),
                nudge.title(), nudge.body(),
                critical.title(), critical.body()
        );
        previewSampleQuote.set(preview);
    }

    public void setQuietHours(boolean enabled, LocalTime start, LocalTime end) {
        AppSettings settings = settingsService.getSettings();
        settings.setQuietHoursEnabled(enabled);
        if (start != null) settings.setQuietHoursStart(start);
        if (end != null) settings.setQuietHoursEnd(end);
        settingsService.updateSettings(settings);

        quietHoursEnabled.set(enabled);
        if (start != null) quietHoursStart.set(start);
        if (end != null) quietHoursEnd.set(end);
    }

    public void pauseReminders(int minutes) {
        settingsService.pauseReminders(minutes);
        remindersPaused.set(true);
    }

    public void resumeReminders() {
        settingsService.resumeReminders();
        remindersPaused.set(false);
    }

    public void setAutoStartOnBoot(boolean enabled) {
        AppSettings settings = settingsService.getSettings();
        settings.setAutoStartOnBoot(enabled);
        settingsService.updateSettings(settings);
        autoStartOnBoot.set(enabled);
        if (startupService != null) {
            startupService.setAutoStartEnabled(enabled);
        }
    }

    public void setEscalationInterval(int minutes) {
        AppSettings settings = settingsService.getSettings();
        settings.setEscalationIntervalMinutes(minutes);
        settingsService.updateSettings(settings);
        escalationIntervalMinutes.set(minutes);
    }

    public void setDefaultSnooze(int minutes) {
        AppSettings settings = settingsService.getSettings();
        settings.setDefaultSnoozeMinutes(minutes);
        settingsService.updateSettings(settings);
        defaultSnoozeMinutes.set(minutes);
    }

    public CompletableFuture<Void> exportBackupAsync(File targetFile) {
        return CompletableFuture.runAsync(() -> {
            dataBackupService.exportBackup(targetFile);
            runOnFxThread(() -> statusMessage.set("Backup exported successfully!"));
        });
    }

    public CompletableFuture<Void> importBackupAsync(File sourceFile) {
        return CompletableFuture.runAsync(() -> {
            dataBackupService.importBackup(sourceFile);
            runOnFxThread(() -> {
                loadSettings();
                statusMessage.set("Backup imported successfully!");
            });
        });
    }

    private void runOnFxThread(Runnable action) {
        try {
            if (Platform.isFxApplicationThread()) {
                action.run();
            } else {
                Platform.runLater(action);
            }
        } catch (IllegalStateException e) {
            action.run();
        }
    }

    public CompletableFuture<com.aarav.didyoudoit.service.UpdateService.UpdateInfo> checkForUpdatesAsync() {
        checkingForUpdate.set(true);
        updateStatusMessage.set("Checking GitHub for newer releases...");

        return updateService.checkForUpdatesAsync().thenApply(info -> {
            runOnFxThread(() -> {
                checkingForUpdate.set(false);
                updateAvailable.set(info.updateAvailable());
                latestVersionString.set(info.latestVersion());
                updateDownloadUrl.set(info.downloadUrl());
                updateMsiUrl.set(info.msiUrl() != null ? info.msiUrl() : "");
                updateZipUrl.set(info.zipUrl() != null ? info.zipUrl() : "");
                updateDebUrl.set(info.debUrl() != null ? info.debUrl() : "");
                updateTarGzUrl.set(info.tarGzUrl() != null ? info.tarGzUrl() : "");
                updateReleaseUrl.set(info.releaseUrl() != null ? info.releaseUrl() : "");

                if (info.updateAvailable()) {
                    updateStatusMessage.set("Update Available! " + info.latestVersion() + " is ready to install.");
                } else {
                    updateStatusMessage.set("You're all set! DidYouDoIt " + info.currentVersion() + " is the latest version.");
                }
            });
            return info;
        });
    }

    public CompletableFuture<Void> downloadAndInstallUpdateAsync() {
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String targetUrl = isWindows ? updateMsiUrl.get() : updateDebUrl.get();
        if (targetUrl == null || targetUrl.isBlank()) {
            targetUrl = updateDownloadUrl.get();
        }
        if (targetUrl == null || targetUrl.isBlank()) {
            updateStatusMessage.set("No download URL found for this release.");
            return CompletableFuture.completedFuture(null);
        }

        downloadingUpdate.set(true);
        downloadProgress.set(0.0);
        updateStatusMessage.set("Downloading update package (0%)...");

        return updateService.downloadAssetAsync(targetUrl, progress -> {
            runOnFxThread(() -> {
                downloadProgress.set(progress);
                updateStatusMessage.set(String.format("Downloading update: %d%%", (int) (progress * 100)));
            });
        }).thenAccept(downloadedFile -> {
            runOnFxThread(() -> {
                downloadingUpdate.set(false);
                updateStatusMessage.set("Download complete! Launching installer...");
                try {
                    if (isWindows && downloadedFile.toString().endsWith(".msi")) {
                        new ProcessBuilder("msiexec.exe", "/i", downloadedFile.toAbsolutePath().toString()).start();
                        updateStatusMessage.set("Installer started! Follow the setup wizard to update in-place.");
                    } else {
                        java.awt.Desktop.getDesktop().open(downloadedFile.toFile());
                        updateStatusMessage.set("Package opened! Follow your system installer prompt.");
                    }
                } catch (Exception ex) {
                    updateStatusMessage.set("Failed to launch installer automatically. Saved to: " + downloadedFile.getFileName());
                }
            });
        }).exceptionally(ex -> {
            runOnFxThread(() -> {
                downloadingUpdate.set(false);
                updateStatusMessage.set("Update download failed: " + ex.getMessage());
            });
            return null;
        });
    }

    // Property getters
    public StringProperty appVersionProperty() { return appVersion; }
    public BooleanProperty checkingForUpdateProperty() { return checkingForUpdate; }
    public BooleanProperty updateAvailableProperty() { return updateAvailable; }
    public StringProperty latestVersionStringProperty() { return latestVersionString; }
    public StringProperty updateDownloadUrlProperty() { return updateDownloadUrl; }
    public StringProperty updateMsiUrlProperty() { return updateMsiUrl; }
    public StringProperty updateZipUrlProperty() { return updateZipUrl; }
    public StringProperty updateDebUrlProperty() { return updateDebUrl; }
    public StringProperty updateTarGzUrlProperty() { return updateTarGzUrl; }
    public StringProperty updateReleaseUrlProperty() { return updateReleaseUrl; }
    public StringProperty updateStatusMessageProperty() { return updateStatusMessage; }
    public BooleanProperty downloadingUpdateProperty() { return downloadingUpdate; }
    public DoubleProperty downloadProgressProperty() { return downloadProgress; }

    public ObjectProperty<PersonalityType> personalityTypeProperty() { return personalityType; }
    public BooleanProperty quietHoursEnabledProperty() { return quietHoursEnabled; }
    public ObjectProperty<LocalTime> quietHoursStartProperty() { return quietHoursStart; }
    public ObjectProperty<LocalTime> quietHoursEndProperty() { return quietHoursEnd; }
    public BooleanProperty remindersPausedProperty() { return remindersPaused; }
    public BooleanProperty autoStartOnBootProperty() { return autoStartOnBoot; }
    public BooleanProperty persistentNaggingEnabledProperty() { return persistentNaggingEnabled; }
    public IntegerProperty escalationIntervalMinutesProperty() { return escalationIntervalMinutes; }
    public IntegerProperty defaultSnoozeMinutesProperty() { return defaultSnoozeMinutes; }
    public StringProperty previewSampleQuoteProperty() { return previewSampleQuote; }
    public StringProperty statusMessageProperty() { return statusMessage; }
}
