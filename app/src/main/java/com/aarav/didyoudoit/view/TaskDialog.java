package com.aarav.didyoudoit.view;

import com.aarav.didyoudoit.model.Priority;
import com.aarav.didyoudoit.model.RecurrenceRule;
import com.aarav.didyoudoit.model.RecurrenceType;
import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.model.TaskCategory;
import com.aarav.didyoudoit.ui.components.AppButton;
import com.aarav.didyoudoit.ui.components.InputField;
import com.aarav.didyoudoit.ui.theme.FontManager;
import com.aarav.didyoudoit.ui.theme.Theme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Custom modal dialog for creating and editing tasks.
 * Implements FR-01, FR-02, FR-10, FR-11, FR-16.
 */
public class TaskDialog extends Stage {

    private final Task originalTask;
    private Task resultTask;

    private final InputField titleField;
    private final InputField descriptionField;
    private final ComboBox<TaskCategory> categoryCombo;
    private final ComboBox<Priority> priorityCombo;
    private final DatePicker datePicker;
    private final ComboBox<String> hourCombo;
    private final ComboBox<String> minuteCombo;
    private final ComboBox<String> amPmCombo;
    private final ComboBox<String> durationHoursCombo;
    private final ComboBox<String> durationMinutesCombo;
    private final Label durationSummaryLabel;
    private final ComboBox<RecurrenceType> recurrenceCombo;
    private final InputField customNagField;

    public TaskDialog(Window owner, Task taskToEdit) {
        initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            initOwner(owner);
        }
        initStyle(StageStyle.UNDECORATED);

        this.originalTask = taskToEdit;

        VBox root = new VBox(Theme.SPACING_MD);
        root.getStyleClass().add("card-elevated");
        root.setPadding(new Insets(Theme.SPACING_LG));
        root.setPrefWidth(540);
        root.setMaxWidth(580);

        // Header Title
        Label headerTitle = new Label(taskToEdit == null ? "Create New Task" : "Edit Task");
        headerTitle.setFont(FontManager.getPrimaryFont(20));
        headerTitle.getStyleClass().add("section-header");

        // 1. Title Input
        this.titleField = new InputField("Task Title *", "e.g. Study DSA, complete report, workout");
        if (taskToEdit != null) {
            this.titleField.setText(taskToEdit.getTitle());
        }

        // 2. Description Input
        this.descriptionField = new InputField("Description (Optional)", "Additional notes, goals, or subtasks");
        if (taskToEdit != null) {
            this.descriptionField.setText(taskToEdit.getDescription());
        }

        // 3. Category & Priority Row
        HBox metaRow = new HBox(Theme.SPACING_MD);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        VBox catBox = new VBox(Theme.SPACING_XS);
        Label catLabel = new Label("Category");
        catLabel.setFont(FontManager.getPrimaryFont(12));
        catLabel.getStyleClass().add("caption-text");
        this.categoryCombo = new ComboBox<>();
        this.categoryCombo.getItems().addAll(TaskCategory.values());
        this.categoryCombo.setValue(taskToEdit != null ? taskToEdit.getCategory() : TaskCategory.WORK);
        this.categoryCombo.getStyleClass().add("input-text");
        this.categoryCombo.setMaxWidth(Double.MAX_VALUE);
        catBox.getChildren().addAll(catLabel, categoryCombo);
        HBox.setHgrow(catBox, javafx.scene.layout.Priority.ALWAYS);

        VBox prioBox = new VBox(Theme.SPACING_XS);
        Label prioLabel = new Label("Priority");
        prioLabel.setFont(FontManager.getPrimaryFont(12));
        prioLabel.getStyleClass().add("caption-text");
        this.priorityCombo = new ComboBox<>();
        this.priorityCombo.getItems().addAll(Priority.values());
        this.priorityCombo.setValue(taskToEdit != null ? taskToEdit.getPriority() : Priority.MEDIUM);
        this.priorityCombo.getStyleClass().add("input-text");
        this.priorityCombo.setMaxWidth(Double.MAX_VALUE);
        prioBox.getChildren().addAll(prioLabel, priorityCombo);
        HBox.setHgrow(prioBox, javafx.scene.layout.Priority.ALWAYS);

        metaRow.getChildren().addAll(catBox, prioBox);

        // 4. Due Date & Granular Time Row
        HBox scheduleRow = new HBox(Theme.SPACING_MD);
        scheduleRow.setAlignment(Pos.CENTER_LEFT);

        VBox dateBox = new VBox(Theme.SPACING_XS);
        Label dateLabel = new Label("Due Date");
        dateLabel.setFont(FontManager.getPrimaryFont(12));
        dateLabel.getStyleClass().add("caption-text");
        this.datePicker = new DatePicker();
        LocalDate initialDate = (taskToEdit != null && taskToEdit.getDueDateTime() != null)
                ? taskToEdit.getDueDateTime().toLocalDate()
                : LocalDate.now();
        this.datePicker.setValue(initialDate);
        this.datePicker.getStyleClass().add("input-text");
        this.datePicker.setMaxWidth(Double.MAX_VALUE);
        dateBox.getChildren().addAll(dateLabel, datePicker);
        HBox.setHgrow(dateBox, javafx.scene.layout.Priority.ALWAYS);

        VBox timeBox = new VBox(Theme.SPACING_XS);
        Label timeLabel = new Label("Due Time (HH : MM AM/PM)");
        timeLabel.setFont(FontManager.getPrimaryFont(12));
        timeLabel.getStyleClass().add("caption-text");

        HBox timePickers = new HBox(Theme.SPACING_XS);
        timePickers.setAlignment(Pos.CENTER_LEFT);

        this.hourCombo = new ComboBox<>();
        for (int h = 1; h <= 12; h++) {
            this.hourCombo.getItems().add(String.format("%02d", h));
        }
        this.hourCombo.getStyleClass().add("input-text");
        this.hourCombo.setPrefWidth(68);

        Label colonLabel = new Label(":");
        colonLabel.setStyle("-fx-font-weight: bold; -fx-padding: 0 1 0 1; -fx-text-fill: -fx-text-primary;");

        this.minuteCombo = new ComboBox<>();
        this.minuteCombo.setEditable(true);
        for (int m = 0; m < 60; m += 5) {
            this.minuteCombo.getItems().add(String.format("%02d", m));
        }
        this.minuteCombo.getStyleClass().add("input-text");
        this.minuteCombo.setPrefWidth(72);

        this.amPmCombo = new ComboBox<>();
        this.amPmCombo.getItems().addAll("AM", "PM");
        this.amPmCombo.getStyleClass().add("input-text");
        this.amPmCombo.setPrefWidth(68);

        timePickers.getChildren().addAll(hourCombo, colonLabel, minuteCombo, amPmCombo);
        setInitialTime(taskToEdit);

        timeBox.getChildren().addAll(timeLabel, timePickers);
        HBox.setHgrow(timeBox, javafx.scene.layout.Priority.ALWAYS);

        scheduleRow.getChildren().addAll(dateBox, timeBox);

        // 5. Variable Focus Duration & Recurrence Row
        HBox durRecRow = new HBox(Theme.SPACING_MD);
        durRecRow.setAlignment(Pos.TOP_LEFT);

        VBox durBox = new VBox(Theme.SPACING_XS);
        durBox.setPadding(new Insets(10, 12, 10, 12));
        durBox.setStyle("-fx-background-color: #FAF7F2; -fx-border-color: #E5DFD7; -fx-border-radius: 10px; -fx-background-radius: 10px;");
        HBox.setHgrow(durBox, javafx.scene.layout.Priority.ALWAYS);

        Label durLabel = new Label("Focus Duration (Variable Hours & Mins)");
        durLabel.setFont(FontManager.getPrimaryFont(12));
        durLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-text-primary;");

        HBox durInputRow = new HBox(4);
        durInputRow.setAlignment(Pos.CENTER_LEFT);

        this.durationHoursCombo = new ComboBox<>();
        this.durationHoursCombo.setEditable(true);
        for (int h = 0; h <= 12; h++) {
            this.durationHoursCombo.getItems().add(String.valueOf(h));
        }
        this.durationHoursCombo.getStyleClass().add("input-text");
        this.durationHoursCombo.setPrefWidth(62);

        Label hrsLabel = new Label("h");
        hrsLabel.setFont(FontManager.getPrimaryFont(12));

        this.durationMinutesCombo = new ComboBox<>();
        this.durationMinutesCombo.setEditable(true);
        for (int m = 0; m < 60; m += 5) {
            this.durationMinutesCombo.getItems().add(String.valueOf(m));
        }
        this.durationMinutesCombo.getStyleClass().add("input-text");
        this.durationMinutesCombo.setPrefWidth(66);

        Label minsLabel = new Label("m");
        minsLabel.setFont(FontManager.getPrimaryFont(12));

        int initMinutes = (taskToEdit != null) ? taskToEdit.getDurationMinutes() : 0;
        this.durationHoursCombo.setValue(String.valueOf(initMinutes / 60));
        this.durationMinutesCombo.setValue(String.valueOf(initMinutes % 60));

        this.durationSummaryLabel = new Label();
        this.durationSummaryLabel.setFont(FontManager.getAccentFont(12));

        Runnable updateSummary = () -> {
            int h = parsePositiveInt(durationHoursCombo.getValue());
            int m = parsePositiveInt(durationMinutesCombo.getValue());
            int total = h * 60 + m;
            if (total <= 0) {
                durationSummaryLabel.setText("No timer (Standard task)");
                durationSummaryLabel.setStyle("-fx-text-fill: #8E8880;");
            } else {
                durationSummaryLabel.setText("⏱ Goal: " + formatDurationDetailed(total));
                durationSummaryLabel.setStyle("-fx-text-fill: #E27D60; -fx-font-weight: bold;");
            }
        };

        this.durationHoursCombo.valueProperty().addListener((obs, oldV, newV) -> updateSummary.run());
        this.durationMinutesCombo.valueProperty().addListener((obs, oldV, newV) -> updateSummary.run());
        updateSummary.run();

        // Quick Preset Chips (None, 15m, 25m, 30m, 45m, 1h, 1.5h, 2h)
        FlowPane presetChips = new FlowPane(4, 4);
        presetChips.setAlignment(Pos.CENTER_LEFT);
        presetChips.getChildren().addAll(
                createPresetChip("None", 0, 0, updateSummary),
                createPresetChip("15m", 0, 15, updateSummary),
                createPresetChip("25m", 0, 25, updateSummary),
                createPresetChip("30m", 0, 30, updateSummary),
                createPresetChip("45m", 0, 45, updateSummary),
                createPresetChip("1h", 1, 0, updateSummary),
                createPresetChip("1.5h", 1, 30, updateSummary),
                createPresetChip("2h", 2, 0, updateSummary)
        );

        durInputRow.getChildren().addAll(durationHoursCombo, hrsLabel, durationMinutesCombo, minsLabel);
        durBox.getChildren().addAll(durLabel, durInputRow, presetChips, durationSummaryLabel);

        VBox recBox = new VBox(Theme.SPACING_XS);
        Label recLabel = new Label("Recurrence Pattern");
        recLabel.setFont(FontManager.getPrimaryFont(12));
        recLabel.getStyleClass().add("caption-text");
        this.recurrenceCombo = new ComboBox<>();
        this.recurrenceCombo.getItems().addAll(RecurrenceType.values());
        RecurrenceType initialRecType = (taskToEdit != null && taskToEdit.getRecurrenceRule() != null)
                ? taskToEdit.getRecurrenceRule().getType()
                : RecurrenceType.NONE;
        this.recurrenceCombo.setValue(initialRecType);
        this.recurrenceCombo.getStyleClass().add("input-text");
        this.recurrenceCombo.setMaxWidth(Double.MAX_VALUE);
        recBox.getChildren().addAll(recLabel, recurrenceCombo);
        HBox.setHgrow(recBox, javafx.scene.layout.Priority.ALWAYS);

        durRecRow.getChildren().addAll(durBox, recBox);

        // 6. Custom Nag Message
        this.customNagField = new InputField(
                "Custom Nagging Message (Optional)",
                "e.g. Stop wasting time and get this done!"
        );
        if (taskToEdit != null && taskToEdit.getCustomNagMessage() != null) {
            this.customNagField.setText(taskToEdit.getCustomNagMessage());
        }

        // Action Buttons Row
        HBox buttonsRow = new HBox(Theme.SPACING_MD);
        buttonsRow.setAlignment(Pos.CENTER_RIGHT);

        AppButton cancelBtn = AppButton.ghost("Cancel");
        cancelBtn.setOnAction(e -> close());

        AppButton saveBtn = AppButton.primary(taskToEdit == null ? "Create Task" : "Save Changes");
        saveBtn.setOnAction(e -> handleSave());

        buttonsRow.getChildren().addAll(cancelBtn, saveBtn);

        root.getChildren().addAll(
                headerTitle,
                titleField,
                descriptionField,
                metaRow,
                scheduleRow,
                durRecRow,
                customNagField,
                buttonsRow
        );

        Scene scene = new Scene(root);
        Theme.applyTheme(scene);
        setScene(scene);
    }

    private void setInitialTime(Task task) {
        LocalTime time = (task != null && task.getDueDateTime() != null)
                ? task.getDueDateTime().toLocalTime()
                : LocalTime.of(18, 0);
        int hour = time.getHour();
        int min = time.getMinute();
        String ampm = hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;

        this.hourCombo.setValue(String.format("%02d", displayHour));
        this.minuteCombo.setValue(String.format("%02d", min));
        this.amPmCombo.setValue(ampm);
    }

    private LocalTime getSelectedTime() {
        try {
            int hour = Integer.parseInt(hourCombo.getValue() != null ? hourCombo.getValue().trim() : "12");
            String minStr = minuteCombo.getValue() != null ? minuteCombo.getValue().trim() : "00";
            String digits = minStr.replaceAll("[^0-9]", "");
            int min = digits.isEmpty() ? 0 : Integer.parseInt(digits);
            min = Math.max(0, Math.min(59, min));
            String ampm = amPmCombo.getValue() != null ? amPmCombo.getValue().trim() : "PM";

            if ("PM".equalsIgnoreCase(ampm) && hour < 12) {
                hour += 12;
            } else if ("AM".equalsIgnoreCase(ampm) && hour == 12) {
                hour = 0;
            }
            return LocalTime.of(Math.max(0, Math.min(23, hour)), min);
        } catch (Exception e) {
            return LocalTime.of(18, 0);
        }
    }

    private Button createPresetChip(String label, int hours, int minutes, Runnable onSelect) {
        Button chip = new Button(label);
        chip.getStyleClass().add("btn-card-action");
        chip.setStyle("-fx-font-size: 11px; -fx-padding: 2 6 2 6;");
        chip.setOnAction(e -> {
            durationHoursCombo.setValue(String.valueOf(hours));
            durationMinutesCombo.setValue(String.valueOf(minutes));
            if (onSelect != null) onSelect.run();
        });
        return chip;
    }

    private int parsePositiveInt(String str) {
        if (str == null || str.isBlank()) return 0;
        try {
            String digits = str.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatDurationDetailed(int totalMinutes) {
        if (totalMinutes <= 0) return "No timer";
        int h = totalMinutes / 60;
        int m = totalMinutes % 60;
        if (h > 0 && m > 0) return h + "h " + m + "m (" + totalMinutes + "m total)";
        if (h > 0) return h + "h (" + totalMinutes + "m total)";
        return m + "m";
    }

    private void handleSave() {
        String title = titleField.getText().trim();
        if (title.isBlank()) {
            titleField.setError("Task title is required");
            return;
        }

        LocalDate date = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();
        LocalTime time = getSelectedTime();
        LocalDateTime dueDateTime = LocalDateTime.of(date, time);

        RecurrenceType recType = recurrenceCombo.getValue();
        RecurrenceRule recRule = new RecurrenceRule(recType, time, null);

        int h = parsePositiveInt(durationHoursCombo.getValue());
        int m = parsePositiveInt(durationMinutesCombo.getValue());
        int durationMins = h * 60 + m;

        int remainingSecs = durationMins * 60;
        boolean timerActive = false;
        if (originalTask != null && originalTask.getDurationMinutes() == durationMins) {
            remainingSecs = originalTask.getTimerRemainingSeconds();
            timerActive = originalTask.isTimerActive();
        }

        Task.Builder builder = (originalTask != null)
                ? Task.builder()
                    .id(originalTask.getId())
                    .createdAt(originalTask.getCreatedAt())
                    .status(originalTask.getStatus())
                    .nagCount(originalTask.getNagCount())
                    .escalationLevel(originalTask.getEscalationLevel())
                    .isTemplate(originalTask.isTemplate())
                    .parentTemplateId(originalTask.getParentTemplateId())
                : Task.builder();

        this.resultTask = builder
                .title(title)
                .description(descriptionField.getText().trim())
                .category(categoryCombo.getValue())
                .priority(priorityCombo.getValue())
                .dueDateTime(dueDateTime)
                .recurrenceRule(recRule)
                .customNagMessage(customNagField.getText().trim())
                .durationMinutes(durationMins)
                .timerRemainingSeconds(remainingSecs)
                .timerActive(timerActive)
                .updatedAt(LocalDateTime.now())
                .build();

        close();
    }

    public static Optional<Task> show(Window owner, Task taskToEdit) {
        TaskDialog dialog = new TaskDialog(owner, taskToEdit);
        dialog.showAndWait();
        return Optional.ofNullable(dialog.resultTask);
    }
}
