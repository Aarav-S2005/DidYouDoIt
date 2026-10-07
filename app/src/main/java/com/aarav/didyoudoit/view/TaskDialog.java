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
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
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
    private final ComboBox<String> durationCombo;
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
        root.setPrefWidth(520);
        root.setMaxWidth(560);

        // Header Title
        Label headerTitle = new Label(taskToEdit == null ? "Create New Task" : "Edit Task");
        headerTitle.setFont(FontManager.getPrimaryFont(20));
        headerTitle.getStyleClass().add("section-header");

        // 1. Title Input
        this.titleField = new InputField("Task Title *", "e.g. Study DSA for 1 hour");
        if (taskToEdit != null) {
            this.titleField.setText(taskToEdit.getTitle());
        }

        // 2. Description Input
        this.descriptionField = new InputField("Description (Optional)", "Additional notes, goals, or topics");
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

        // 5. Duration & Recurrence Row
        HBox durRecRow = new HBox(Theme.SPACING_MD);
        durRecRow.setAlignment(Pos.CENTER_LEFT);

        VBox durBox = new VBox(Theme.SPACING_XS);
        Label durLabel = new Label("Focus Duration (Timer)");
        durLabel.setFont(FontManager.getPrimaryFont(12));
        durLabel.getStyleClass().add("caption-text");

        this.durationCombo = new ComboBox<>();
        this.durationCombo.setEditable(true);
        this.durationCombo.getItems().addAll(
                "None",
                "15 minutes",
                "25 minutes (Pomodoro)",
                "30 minutes",
                "45 minutes",
                "1 hour (60 min)",
                "1.5 hours (90 min)",
                "2 hours (120 min)",
                "3 hours (180 min)"
        );
        this.durationCombo.setValue(formatInitialDuration(taskToEdit != null ? taskToEdit.getDurationMinutes() : 0));
        this.durationCombo.getStyleClass().add("input-text");
        this.durationCombo.setMaxWidth(Double.MAX_VALUE);
        durBox.getChildren().addAll(durLabel, durationCombo);
        HBox.setHgrow(durBox, javafx.scene.layout.Priority.ALWAYS);

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

    private String formatInitialDuration(int minutes) {
        if (minutes <= 0) return "None";
        if (minutes == 15) return "15 minutes";
        if (minutes == 25) return "25 minutes (Pomodoro)";
        if (minutes == 30) return "30 minutes";
        if (minutes == 45) return "45 minutes";
        if (minutes == 60) return "1 hour (60 min)";
        if (minutes == 90) return "1.5 hours (90 min)";
        if (minutes == 120) return "2 hours (120 min)";
        if (minutes == 180) return "3 hours (180 min)";
        return minutes + " minutes";
    }

    private int parseDurationMinutes(String str) {
        if (str == null || str.isBlank() || str.equalsIgnoreCase("None")) {
            return 0;
        }
        String clean = str.trim().toLowerCase();
        if (clean.contains("1.5 hour") || clean.contains("90 min")) return 90;
        if (clean.contains("2.5 hour") || clean.contains("150 min")) return 150;
        if (clean.contains("1 hour") || clean.contains("60 min")) return 60;
        if (clean.contains("2 hour") || clean.contains("120 min")) return 120;
        if (clean.contains("3 hour") || clean.contains("180 min")) return 180;
        if (clean.contains("15 min")) return 15;
        if (clean.contains("25 min")) return 25;
        if (clean.contains("30 min")) return 30;
        if (clean.contains("45 min")) return 45;

        try {
            if (clean.endsWith("h") || clean.endsWith("hr") || clean.endsWith("hours") || clean.endsWith("hour")) {
                String num = clean.replaceAll("[^0-9.]", "");
                double h = Double.parseDouble(num);
                return (int) Math.round(h * 60);
            }
            String num = clean.replaceAll("[^0-9]", "");
            if (!num.isBlank()) {
                return Integer.parseInt(num);
            }
        } catch (Exception ignored) {}
        return 0;
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

        int durationMins = parseDurationMinutes(durationCombo.getValue());
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
