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

        this.contentBox.getChildren().addAll(
                headerBox,
                personalitySection,
                quietHoursSection,
                timingSection,
                backupSection
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
        Label header = new Label("Quiet Hours & Pause Reminders");
        header.setFont(FontManager.getPrimaryFont(18));
        header.getStyleClass().add("section-header");

        // Quiet hours toggle and time pickers
        VBox quietBox = new VBox(Theme.SPACING_SM);
        quietBox.setPadding(new Insets(Theme.SPACING_MD));
        quietBox.getStyleClass().add("card");

        CheckBox quietCheck = new CheckBox("Enable Quiet Hours (suppress all reminders during this window)");
        quietCheck.setFont(FontManager.getPrimaryFont(13));
        quietCheck.setSelected(viewModel.quietHoursEnabledProperty().get());

        HBox timeRow = new HBox(Theme.SPACING_MD);
        timeRow.setAlignment(Pos.CENTER_LEFT);

        Label startLabel = new Label("Start Time:");
        startLabel.setFont(FontManager.getPrimaryFont(12));
        ComboBox<Integer> startHourCombo = createHourComboBox(viewModel.quietHoursStartProperty().get().getHour());

        Label endLabel = new Label("End Time:");
        endLabel.setFont(FontManager.getPrimaryFont(12));
        ComboBox<Integer> endHourCombo = createHourComboBox(viewModel.quietHoursEndProperty().get().getHour());

        timeRow.getChildren().addAll(startLabel, startHourCombo, endLabel, endHourCombo);

        Runnable saveQuietHours = () -> {
            boolean enabled = quietCheck.isSelected();
            int startH = startHourCombo.getValue() != null ? startHourCombo.getValue() : 22;
            int endH = endHourCombo.getValue() != null ? endHourCombo.getValue() : 8;
            viewModel.setQuietHours(enabled, LocalTime.of(startH, 0), LocalTime.of(endH, 0));
        };

        quietCheck.setOnAction(e -> saveQuietHours.run());
        startHourCombo.setOnAction(e -> saveQuietHours.run());
        endHourCombo.setOnAction(e -> saveQuietHours.run());

        // Quick Pause Reminders Buttons (All text, no icons)
        Label pauseLabel = new Label("Quick Pause:");
        pauseLabel.setFont(FontManager.getPrimaryFont(13));
        pauseLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2B2D42;");

        HBox pauseBtnRow = new HBox(Theme.SPACING_SM);
        pauseBtnRow.setAlignment(Pos.CENTER_LEFT);

        Button pause30Btn = createTextActionButton("Pause 30m", e -> {
            viewModel.pauseReminders(30);
            if (toastNotifier != null) toastNotifier.accept("Paused", "Reminders paused for 30 minutes.");
        });

        Button pause1hBtn = createTextActionButton("Pause 1 Hour", e -> {
            viewModel.pauseReminders(60);
            if (toastNotifier != null) toastNotifier.accept("Paused", "Reminders paused for 1 hour.");
        });

        Button pause2hBtn = createTextActionButton("Pause 2 Hours", e -> {
            viewModel.pauseReminders(120);
            if (toastNotifier != null) toastNotifier.accept("Paused", "Reminders paused for 2 hours.");
        });

        Button resumeBtn = createTextActionButton("Resume Now", e -> {
            viewModel.resumeReminders();
            if (toastNotifier != null) toastNotifier.accept("Resumed", "Reminders are active.");
        });

        pauseBtnRow.getChildren().addAll(pause30Btn, pause1hBtn, pause2hBtn, resumeBtn);

        quietBox.getChildren().addAll(quietCheck, timeRow, new Separator(), pauseLabel, pauseBtnRow);
        section.getChildren().addAll(header, quietBox);
        return section;
    }

    private VBox createTimingSection() {
        VBox section = new VBox(Theme.SPACING_MD);
        Label header = new Label("Nagging Frequency & Startup");
        header.setFont(FontManager.getPrimaryFont(18));
        header.getStyleClass().add("section-header");

        VBox box = new VBox(Theme.SPACING_MD);
        box.setPadding(new Insets(Theme.SPACING_MD));
        box.getStyleClass().add("card");

        // Auto-start on boot checkbox
        CheckBox autoStartCheck = new CheckBox("Launch DidYouDoIt automatically when Windows starts");
        autoStartCheck.setFont(FontManager.getPrimaryFont(13));
        autoStartCheck.setSelected(viewModel.autoStartOnBootProperty().get());
        autoStartCheck.setOnAction(e -> {
            viewModel.setAutoStartOnBoot(autoStartCheck.isSelected());
            if (toastNotifier != null) {
                toastNotifier.accept("Startup Preference", autoStartCheck.isSelected() ? "Auto-start enabled" : "Auto-start disabled");
            }
        });

        // Escalation interval combo
        HBox intervalRow = new HBox(Theme.SPACING_MD);
        intervalRow.setAlignment(Pos.CENTER_LEFT);
        Label intervalLabel = new Label("Escalate Overdue Tasks Every:");
        intervalLabel.setFont(FontManager.getPrimaryFont(12));

        ComboBox<String> intervalCombo = new ComboBox<>();
        intervalCombo.getItems().addAll("5 minutes", "10 minutes", "15 minutes", "30 minutes");
        intervalCombo.setValue(viewModel.escalationIntervalMinutesProperty().get() + " minutes");
        intervalCombo.setOnAction(e -> {
            String val = intervalCombo.getValue();
            if (val != null) {
                int mins = Integer.parseInt(val.replace(" minutes", ""));
                viewModel.setEscalationInterval(mins);
            }
        });

        intervalRow.getChildren().addAll(intervalLabel, intervalCombo);

        // Default snooze combo
        HBox snoozeRow = new HBox(Theme.SPACING_MD);
        snoozeRow.setAlignment(Pos.CENTER_LEFT);
        Label snoozeLabel = new Label("Default Snooze Duration:");
        snoozeLabel.setFont(FontManager.getPrimaryFont(12));

        ComboBox<String> snoozeCombo = new ComboBox<>();
        snoozeCombo.getItems().addAll("5 minutes", "10 minutes", "15 minutes", "30 minutes", "60 minutes");
        snoozeCombo.setValue(viewModel.defaultSnoozeMinutesProperty().get() + " minutes");
        snoozeCombo.setOnAction(e -> {
            String val = snoozeCombo.getValue();
            if (val != null) {
                int mins = Integer.parseInt(val.replace(" minutes", ""));
                viewModel.setDefaultSnooze(mins);
            }
        });

        snoozeRow.getChildren().addAll(snoozeLabel, snoozeCombo);

        box.getChildren().addAll(autoStartCheck, new Separator(), intervalRow, snoozeRow);
        section.getChildren().addAll(header, box);
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

    private ComboBox<Integer> createHourComboBox(int initialHour) {
        ComboBox<Integer> combo = new ComboBox<>();
        for (int i = 0; i < 24; i++) {
            combo.getItems().add(i);
        }
        combo.setValue(initialHour);
        return combo;
    }

    private Button createTextActionButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.getStyleClass().add("btn-card-action");
        btn.setOnAction(handler);
        return btn;
    }
}
