package com.aarav.didyoudoit.view;

import com.aarav.didyoudoit.model.PersonalityType;
import com.aarav.didyoudoit.ui.components.AppButton;
import com.aarav.didyoudoit.ui.theme.FontManager;
import com.aarav.didyoudoit.ui.theme.Theme;
import com.aarav.didyoudoit.viewmodel.SettingsViewModel;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.time.LocalTime;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Settings and customization view.
 * Adheres strictly to the "No icons, buttons with text instead" rule and "No FXML".
 * Implements FR-15, FR-16, FR-17, FR-20, FR-24, FR-25, NFR-08.
 */
public class SettingsView extends ScrollPane {

    private final SettingsViewModel viewModel;
    private final BiConsumer<String, String> toastNotifier;
    private final VBox contentBox;

    public SettingsView(SettingsViewModel viewModel, BiConsumer<String, String> toastNotifier) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel cannot be null");
        this.toastNotifier = toastNotifier;

        setFitToWidth(true);
        getStyleClass().add("scroll-pane");

        this.contentBox = new VBox(Theme.SPACING_LG);
        this.contentBox.setPadding(new Insets(Theme.SPACING_LG));
        this.contentBox.getStyleClass().add("main-content-pane");
        this.contentBox.setMaxWidth(Double.MAX_VALUE);

        // 1. Header
        VBox headerBox = createHeaderBox();

        // 2. Personality Selection Section (FR-15, FR-16)
        VBox personalitySection = createPersonalitySection();

        // 3. Quiet Hours & Pause Section (FR-17)
        VBox quietHoursSection = createQuietHoursSection();

        // 4. Timing & Behavior Section (FR-20, FR-24)
        VBox timingSection = createTimingSection();

        // 5. Backup & Restore Section (FR-25)
        VBox backupSection = createBackupSection();

        // 6. Application Updates & About Section
        VBox updateSection = createUpdateSection();

        this.contentBox.getChildren().addAll(
                headerBox,
                personalitySection,
                quietHoursSection,
                timingSection,
                backupSection,
                updateSection
        );

        setContent(contentBox);
    }

    private VBox createHeaderBox() {
        VBox box = new VBox(4);
        Label title = new Label("Application Settings");
        title.setFont(FontManager.getPrimaryFont(24));
        title.getStyleClass().add("section-header");

        Label subtitle = new Label("Configure nagging personalities, quiet hours, escalation timing, and backups.");
        subtitle.setFont(FontManager.getAccentFont(14));
        subtitle.getStyleClass().add("caption-text");

        box.getChildren().addAll(title, subtitle);
        return box;
    }

    private VBox createPersonalitySection() {
        VBox section = new VBox(Theme.SPACING_MD);
        Label header = new Label("Nagging Personality Tone");
        header.setFont(FontManager.getPrimaryFont(18));
        header.getStyleClass().add("section-header");

        Label sub = new Label("Choose the attitude DidYouDoIt uses when nudging you for overdue tasks.");
        sub.setFont(FontManager.getPrimaryFont(12));
        sub.getStyleClass().add("caption-text");

        // Personality Cards FlowPane
        FlowPane cardsFlow = new FlowPane(Theme.SPACING_MD, Theme.SPACING_MD);
        cardsFlow.setMaxWidth(Double.MAX_VALUE);

        for (PersonalityType type : PersonalityType.values()) {
            VBox card = createPersonalityCard(type, cardsFlow);
            cardsFlow.getChildren().add(card);
        }

        // Live Preview Quote Box (FR-16)
        VBox previewBox = new VBox(Theme.SPACING_XS);
        previewBox.setPadding(new Insets(Theme.SPACING_MD));
        previewBox.getStyleClass().add("card");

        Label previewTitle = new Label("Live Personality Tone Preview");
        previewTitle.setFont(FontManager.getPrimaryFont(13));
        previewTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #E27D60;");

        Label previewText = new Label();
        previewText.setFont(FontManager.getPrimaryFont(12));
        previewText.setStyle("-fx-text-fill: #2B2D42; -fx-line-spacing: 4px;");
        previewText.setWrapText(true);
        previewText.textProperty().bind(viewModel.previewSampleQuoteProperty());

        previewBox.getChildren().addAll(previewTitle, previewText);

        section.getChildren().addAll(header, sub, cardsFlow, previewBox);
        return section;
    }

    private VBox createPersonalityCard(PersonalityType type, FlowPane parentFlow) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(Theme.SPACING_MD));
        card.getStyleClass().add("card");
        card.setMinWidth(190);
        card.setPrefWidth(220);
        card.setCursor(javafx.scene.Cursor.HAND);

        Label name = new Label(type.getDisplayName());
        name.setFont(FontManager.getPrimaryFont(15));
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: #2B2D42;");

        Label desc = new Label(type.getDescription());
        desc.setFont(FontManager.getPrimaryFont(11));
        desc.setStyle("-fx-text-fill: #6B7280;");
        desc.setWrapText(true);

        card.getChildren().addAll(name, desc);

        // Update active highlight based on ViewModel
        Runnable updateBorder = () -> {
            boolean isSelected = viewModel.personalityTypeProperty().get() == type;
            if (isSelected) {
                card.setStyle("-fx-border-color: #E27D60; -fx-border-width: 2px; -fx-background-color: #FFF9F7;");
            } else {
                card.setStyle("-fx-border-color: #E5DFD7; -fx-border-width: 1px; -fx-background-color: #FFFFFF;");
            }
        };

        viewModel.personalityTypeProperty().addListener((obs, oldVal, newVal) -> updateBorder.run());
        updateBorder.run();

        card.setOnMouseClicked(e -> {
            viewModel.setPersonality(type);
            if (toastNotifier != null) {
                toastNotifier.accept("Tone Updated", "Switched to " + type.getDisplayName());
            }
        });

        return card;
    }

    private VBox createQuietHoursSection() {
        VBox section = new VBox(Theme.SPACING_MD);
        Label header = new Label("Quiet Hours & Do Not Disturb");
        header.setFont(FontManager.getPrimaryFont(18));
        header.getStyleClass().add("section-header");

        Label sectionDesc = new Label("Automatically silence all popups and notifications so your sleep or focus time isn't interrupted.");
        sectionDesc.setFont(FontManager.getPrimaryFont(12));
        sectionDesc.getStyleClass().add("caption-text");

        VBox quietBox = new VBox(Theme.SPACING_MD);
        quietBox.setPadding(new Insets(Theme.SPACING_MD));
        quietBox.getStyleClass().add("card");

        // Main Toggle
        CheckBox quietCheck = new CheckBox("Enable Scheduled Do Not Disturb");
        quietCheck.setFont(FontManager.getPrimaryFont(14));
        quietCheck.setStyle("-fx-font-weight: bold;");
        quietCheck.setSelected(viewModel.quietHoursEnabledProperty().get());

        // Time pickers with readable 12-hour AM/PM labels
        GridPane timeGrid = new GridPane();
        timeGrid.setHgap(Theme.SPACING_LG);
        timeGrid.setVgap(Theme.SPACING_SM);
        timeGrid.setAlignment(Pos.CENTER_LEFT);

        Label startLabel = new Label("Quiet Hours Start (Bedtime / Silence begins):");
        startLabel.setFont(FontManager.getPrimaryFont(12));
        ComboBox<String> startHourCombo = create12HourComboBox(viewModel.quietHoursStartProperty().get().getHour());

        Label endLabel = new Label("Quiet Hours End (Morning / Reminders resume):");
        endLabel.setFont(FontManager.getPrimaryFont(12));
        ComboBox<String> endHourCombo = create12HourComboBox(viewModel.quietHoursEndProperty().get().getHour());

        timeGrid.add(startLabel, 0, 0);
        timeGrid.add(startHourCombo, 0, 1);
        timeGrid.add(endLabel, 1, 0);
        timeGrid.add(endHourCombo, 1, 1);

        Label statusNotice = new Label();
        statusNotice.setFont(FontManager.getAccentFont(12.5));
        statusNotice.setStyle("-fx-text-fill: #E27D60; -fx-font-style: italic;");

        Runnable updateStatusNotice = () -> {
            if (quietCheck.isSelected()) {
                statusNotice.setText("Notifications will be completely silenced between " + startHourCombo.getValue() + " and " + endHourCombo.getValue() + " every day.");
            } else {
                statusNotice.setText("Scheduled quiet hours are currently disabled (reminders will fire at all scheduled times).");
            }
        };

        Runnable saveQuietHours = () -> {
            boolean enabled = quietCheck.isSelected();
            int startH = parse12HourString(startHourCombo.getValue(), 22);
            int endH = parse12HourString(endHourCombo.getValue(), 8);
            viewModel.setQuietHours(enabled, LocalTime.of(startH, 0), LocalTime.of(endH, 0));
            updateStatusNotice.run();
        };

        quietCheck.setOnAction(e -> saveQuietHours.run());
        startHourCombo.setOnAction(e -> saveQuietHours.run());
        endHourCombo.setOnAction(e -> saveQuietHours.run());
        updateStatusNotice.run();

        // Quick Temporary Pause Section
        VBox pauseSubBox = new VBox(Theme.SPACING_XS);
        Label pauseHeader = new Label("Take a Quick Break (Temporary Pause)");
        pauseHeader.setFont(FontManager.getPrimaryFont(13));
        pauseHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #2B2D42;");

        Label pauseSub = new Label("In a meeting or need silence right now? Pause all reminders temporarily without modifying your daily schedule.");
        pauseSub.setFont(FontManager.getPrimaryFont(11.5));
        pauseSub.setStyle("-fx-text-fill: #6B7280;");

        HBox pauseBtnRow = new HBox(Theme.SPACING_SM);
        pauseBtnRow.setAlignment(Pos.CENTER_LEFT);

        Button pause30Btn = createTextActionButton("Pause for 30m", e -> {
            viewModel.pauseReminders(30);
            if (toastNotifier != null) toastNotifier.accept("Reminders Paused", "All nagging paused for 30 minutes.");
        });

        Button pause1hBtn = createTextActionButton("Pause for 1 Hour", e -> {
            viewModel.pauseReminders(60);
            if (toastNotifier != null) toastNotifier.accept("Reminders Paused", "All nagging paused for 1 hour.");
        });

        Button pause2hBtn = createTextActionButton("Pause for 2 Hours", e -> {
            viewModel.pauseReminders(120);
            if (toastNotifier != null) toastNotifier.accept("Reminders Paused", "All nagging paused for 2 hours.");
        });

        Button resumeBtn = createTextActionButton("Resume Reminders Now", e -> {
            viewModel.resumeReminders();
            if (toastNotifier != null) toastNotifier.accept("Reminders Active", "Nagging daemon is now active.");
        });

        pauseBtnRow.getChildren().addAll(pause30Btn, pause1hBtn, pause2hBtn, resumeBtn);
        pauseSubBox.getChildren().addAll(pauseHeader, pauseSub, pauseBtnRow);

        quietBox.getChildren().addAll(quietCheck, timeGrid, statusNotice, new Separator(), pauseSubBox);
        section.getChildren().addAll(header, sectionDesc, quietBox);
        return section;
    }

    private VBox createTimingSection() {
        VBox section = new VBox(Theme.SPACING_MD);
        Label header = new Label("Nagging Frequency & Startup Preferences");
        header.setFont(FontManager.getPrimaryFont(18));
        header.getStyleClass().add("section-header");

        Label sectionDesc = new Label("Control how persistently DidYouDoIt follows up on overdue tasks.");
        sectionDesc.setFont(FontManager.getPrimaryFont(12));
        sectionDesc.getStyleClass().add("caption-text");

        VBox box = new VBox(Theme.SPACING_MD);
        box.setPadding(new Insets(Theme.SPACING_MD));
        box.getStyleClass().add("card");

        // Auto-start on boot checkbox
        CheckBox autoStartCheck = new CheckBox("Launch DidYouDoIt automatically in the background on system login");
        autoStartCheck.setFont(FontManager.getPrimaryFont(13));
        autoStartCheck.setSelected(viewModel.autoStartOnBootProperty().get());
        autoStartCheck.setOnAction(e -> {
            viewModel.setAutoStartOnBoot(autoStartCheck.isSelected());
            if (toastNotifier != null) {
                toastNotifier.accept("Startup Preference", autoStartCheck.isSelected() ? "Auto-start enabled" : "Auto-start disabled");
            }
        });

        // Escalation interval combo
        VBox intervalBox = new VBox(Theme.SPACING_XS);
        Label intervalLabel = new Label("Period (Nagging Frequency):");
        intervalLabel.setFont(FontManager.getPrimaryFont(13));
        intervalLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-text-primary;");

        Label intervalHelp = new Label("How frequently the app nudges you while an active task remains overdue");
        intervalHelp.setFont(FontManager.getPrimaryFont(11.5));
        intervalHelp.getStyleClass().add("caption-text");

        ComboBox<String> intervalCombo = new ComboBox<>();
        intervalCombo.getItems().addAll(
                "Every 5 minutes (Intense Accountability)",
                "Every 10 minutes (Recommended)",
                "Every 15 minutes (Balanced)",
                "Every 30 minutes (Gentle & Relaxed)"
        );

        int currentInterval = viewModel.escalationIntervalMinutesProperty().get();
        if (currentInterval == 5) intervalCombo.setValue("Every 5 minutes (Intense Accountability)");
        else if (currentInterval == 15) intervalCombo.setValue("Every 15 minutes (Balanced)");
        else if (currentInterval == 30) intervalCombo.setValue("Every 30 minutes (Gentle & Relaxed)");
        else intervalCombo.setValue("Every 10 minutes (Recommended)");

        intervalCombo.setOnAction(e -> {
            String val = intervalCombo.getValue();
            if (val != null) {
                if (val.contains("5 minutes")) viewModel.setEscalationInterval(5);
                else if (val.contains("15 minutes")) viewModel.setEscalationInterval(15);
                else if (val.contains("30 minutes")) viewModel.setEscalationInterval(30);
                else viewModel.setEscalationInterval(10);
            }
        });

        intervalBox.getChildren().addAll(intervalLabel, intervalHelp, intervalCombo);

        // Default snooze combo
        VBox snoozeBox = new VBox(Theme.SPACING_XS);
        Label snoozeLabel = new Label("Snooze Duration:");
        snoozeLabel.setFont(FontManager.getPrimaryFont(13));
        snoozeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-text-primary;");

        Label snoozeHelp = new Label("How long reminders pause when you click 'Snooze' on a notification or task card");
        snoozeHelp.setFont(FontManager.getPrimaryFont(11.5));
        snoozeHelp.getStyleClass().add("caption-text");

        ComboBox<String> snoozeCombo = new ComboBox<>();
        snoozeCombo.getItems().addAll("5 minutes", "10 minutes", "15 minutes (Recommended)", "30 minutes", "60 minutes");

        int currentSnooze = viewModel.defaultSnoozeMinutesProperty().get();
        if (currentSnooze == 5) snoozeCombo.setValue("5 minutes");
        else if (currentSnooze == 10) snoozeCombo.setValue("10 minutes");
        else if (currentSnooze == 30) snoozeCombo.setValue("30 minutes");
        else if (currentSnooze == 60) snoozeCombo.setValue("60 minutes");
        else snoozeCombo.setValue("15 minutes (Recommended)");

        snoozeCombo.setOnAction(e -> {
            String val = snoozeCombo.getValue();
            if (val != null) {
                int mins = Integer.parseInt(val.replaceAll("[^0-9]", ""));
                viewModel.setDefaultSnooze(mins);
            }
        });

        snoozeBox.getChildren().addAll(snoozeLabel, snoozeHelp, snoozeCombo);

        box.getChildren().addAll(autoStartCheck, new Separator(), intervalBox, snoozeBox);
        section.getChildren().addAll(header, sectionDesc, box);
        return section;
    }

    private VBox createBackupSection() {
        VBox section = new VBox(Theme.SPACING_MD);
        Label header = new Label("Data Backup & Restore");
        header.setFont(FontManager.getPrimaryFont(18));
        header.getStyleClass().add("section-header");

        VBox box = new VBox(Theme.SPACING_MD);
        box.setPadding(new Insets(Theme.SPACING_MD));
        box.getStyleClass().add("card");

        Label desc = new Label("Export your entire task library, habits, and preferences to a JSON file, or restore from a previous backup.");
        desc.setFont(FontManager.getPrimaryFont(12));
        desc.getStyleClass().add("caption-text");

        HBox btnRow = new HBox(Theme.SPACING_MD);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        AppButton exportBtn = AppButton.secondary("Export Backup to JSON");
        exportBtn.setOnAction(e -> handleExport());

        AppButton importBtn = AppButton.ghost("Import Backup from JSON");
        importBtn.setOnAction(e -> handleImport());

        btnRow.getChildren().addAll(exportBtn, importBtn);
        box.getChildren().addAll(desc, btnRow);
        section.getChildren().addAll(header, box);
        return section;
    }

    private void handleExport() {
        Window window = getScene() != null ? getScene().getWindow() : null;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save DidYouDoIt Backup");
        fileChooser.setInitialFileName("didyoudoit-backup.json");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));

        File file = fileChooser.showSaveDialog(window);
        if (file != null) {
            viewModel.exportBackupAsync(file).thenRun(() -> {
                if (toastNotifier != null) {
                    Platform.runLater(() -> toastNotifier.accept("Export Complete", "Data saved to " + file.getName()));
                }
            });
        }
    }

    private void handleImport() {
        Window window = getScene() != null ? getScene().getWindow() : null;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select DidYouDoIt Backup File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));

        File file = fileChooser.showOpenDialog(window);
        if (file != null) {
            viewModel.importBackupAsync(file).thenRun(() -> {
                if (toastNotifier != null) {
                    Platform.runLater(() -> toastNotifier.accept("Import Complete", "Restored data from " + file.getName()));
                }
            });
        }
    }

    private VBox createUpdateSection() {
        VBox section = new VBox(Theme.SPACING_MD);
        Label header = new Label("Software Updates & Version");
        header.setFont(FontManager.getPrimaryFont(18));
        header.getStyleClass().add("section-header");

        Label sectionDesc = new Label("Check for new features, nagging copy enhancements, and bug fixes directly from GitHub.");
        sectionDesc.setFont(FontManager.getPrimaryFont(12));
        sectionDesc.getStyleClass().add("caption-text");

        VBox box = new VBox(Theme.SPACING_MD);
        box.setPadding(new Insets(Theme.SPACING_MD));
        box.getStyleClass().add("card");

        HBox versionRow = new HBox(Theme.SPACING_MD);
        versionRow.setAlignment(Pos.CENTER_LEFT);

        Label currentVerLabel = new Label("Installed Version:");
        currentVerLabel.setFont(FontManager.getPrimaryFont(13));
        currentVerLabel.setStyle("-fx-font-weight: bold;");

        Label currentVerBadge = new Label("v" + viewModel.appVersionProperty().get());
        currentVerBadge.getStyleClass().addAll("badge", "badge-default-chip");

        versionRow.getChildren().addAll(currentVerLabel, currentVerBadge);

        Label statusLabel = new Label();
        statusLabel.setFont(FontManager.getPrimaryFont(12.5));
        statusLabel.textProperty().bind(viewModel.updateStatusMessageProperty());

        HBox actionsRow = new HBox(Theme.SPACING_MD);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        AppButton checkBtn = AppButton.secondary("Check for Updates Now");
        checkBtn.disableProperty().bind(viewModel.checkingForUpdateProperty());
        checkBtn.setOnAction(e -> {
            viewModel.checkForUpdatesAsync().thenAccept(info -> {
                if (toastNotifier != null) {
                    Platform.runLater(() -> {
                        if (info.updateAvailable()) {
                            toastNotifier.accept("New Version Found!", info.latestVersion() + " is available for download.");
                        } else {
                            toastNotifier.accept("Up to Date", "You are running the latest release.");
                        }
                    });
                }
            });
        });

        AppButton downloadBtn = AppButton.primary("Download Latest Release");
        downloadBtn.visibleProperty().bind(viewModel.updateAvailableProperty());
        downloadBtn.managedProperty().bind(viewModel.updateAvailableProperty());
        downloadBtn.setOnAction(e -> {
            String url = viewModel.updateDownloadUrlProperty().get();
            if (url == null || url.isBlank()) {
                url = "https://github.com/Aarav-S2005/DidYouDoIt/releases";
            }
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
            } catch (Exception ex) {
                if (toastNotifier != null) toastNotifier.accept("Open Link Failed", url);
            }
        });

        actionsRow.getChildren().addAll(checkBtn, downloadBtn);

        box.getChildren().addAll(versionRow, statusLabel, actionsRow);
        section.getChildren().addAll(header, sectionDesc, box);
        return section;
    }

    private ComboBox<String> create12HourComboBox(int initialHour) {
        ComboBox<String> combo = new ComboBox<>();
        for (int i = 0; i < 24; i++) {
            combo.getItems().add(format12HourString(i));
        }
        combo.setValue(format12HourString(initialHour));
        return combo;
    }

    private static String format12HourString(int hour24) {
        if (hour24 == 0) return "12:00 AM (Midnight)";
        if (hour24 == 12) return "12:00 PM (Noon)";
        if (hour24 < 12) return String.format("%02d:00 AM", hour24);
        return String.format("%02d:00 PM", hour24 - 12);
    }

    private static int parse12HourString(String formatted, int fallback) {
        if (formatted == null) return fallback;
        if (formatted.contains("Midnight")) return 0;
        if (formatted.contains("Noon")) return 12;

        try {
            String clean = formatted.replaceAll("[^0-9APMapm]", "").trim();
            boolean isPm = formatted.toUpperCase().contains("PM");
            int hour = Integer.parseInt(formatted.substring(0, 2).trim());
            if (isPm && hour < 12) hour += 12;
            if (!isPm && hour == 12) hour = 0;
            return hour;
        } catch (Exception e) {
            return fallback;
        }
    }

    private Button createTextActionButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.getStyleClass().add("btn-card-action");
        btn.setOnAction(handler);
        return btn;
    }
}
