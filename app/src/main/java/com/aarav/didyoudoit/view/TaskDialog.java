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
    private final ComboBox<String> timeCombo;
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
        root.setPrefWidth(480);
        root.setMaxWidth(540);

        // Header Title
        Label headerTitle = new Label(taskToEdit == null ? "Create New Task" : "Edit Task");
        headerTitle.setFont(FontManager.getPrimaryFont(20));
        headerTitle.getStyleClass().add("section-header");

        // 1. Title Input
        this.titleField = new InputField("Task Title *", "e.g. Complete economics assignment");
        if (taskToEdit != null) {
            this.titleField.setText(taskToEdit.getTitle());
        }

        // 2. Description Input
        this.descriptionField = new InputField("Description (Optional)", "Additional notes or subtasks");
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
        prioBox.getChildren().addAll(prioLabel, priorityCombo);
        HBox.setHgrow(prioBox, javafx.scene.layout.Priority.ALWAYS);

        metaRow.getChildren().addAll(catBox, prioBox);

        // 4. Due Date & Time Row
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
        dateBox.getChildren().addAll(dateLabel, datePicker);
        HBox.setHgrow(dateBox, javafx.scene.layout.Priority.ALWAYS);

        VBox timeBox = new VBox(Theme.SPACING_XS);
        Label timeLabel = new Label("Due Time");
        timeLabel.setFont(FontManager.getPrimaryFont(12));
        timeLabel.getStyleClass().add("caption-text");
        this.timeCombo = new ComboBox<>();
        this.timeCombo.getItems().addAll(
                "09:00 AM", "12:00 PM", "03:00 PM", "06:00 PM", "09:00 PM", "11:59 PM"
        );
        this.timeCombo.setValue(formatInitialTime(taskToEdit));
        this.timeCombo.getStyleClass().add("input-text");
        timeBox.getChildren().addAll(timeLabel, timeCombo);
        HBox.setHgrow(timeBox, javafx.scene.layout.Priority.ALWAYS);

        scheduleRow.getChildren().addAll(dateBox, timeBox);

        // 5. Recurrence Rule
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
                recBox,
                customNagField,
                buttonsRow
        );

        Scene scene = new Scene(root);
        Theme.applyTheme(scene);
        setScene(scene);
    }

    private String formatInitialTime(Task task) {
        if (task == null || task.getDueDateTime() == null) {
            return "06:00 PM";
        }
        LocalTime time = task.getDueDateTime().toLocalTime();
        int hour = time.getHour();
        int minute = time.getMinute();
        String ampm = hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        return String.format("%02d:%02d %s", displayHour, minute, ampm);
    }

    private LocalTime parseSelectedTime(String str) {
        if (str == null || str.isBlank()) {
            return LocalTime.of(18, 0);
        }
        try {
            String[] parts = str.trim().split(" ");
            String[] hm = parts[0].split(":");
            int hour = Integer.parseInt(hm[0]);
            int minute = Integer.parseInt(hm[1]);
            if (parts.length > 1 && parts[1].equalsIgnoreCase("PM") && hour < 12) {
                hour += 12;
            } else if (parts.length > 1 && parts[1].equalsIgnoreCase("AM") && hour == 12) {
                hour = 0;
            }
            return LocalTime.of(hour, minute);
        } catch (Exception e) {
            return LocalTime.of(18, 0);
        }
    }

    private void handleSave() {
        String title = titleField.getText().trim();
        if (title.isBlank()) {
            titleField.setError("Task title is required");
            return;
        }

        LocalDate date = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();
        LocalTime time = parseSelectedTime(timeCombo.getValue());
        LocalDateTime dueDateTime = LocalDateTime.of(date, time);

        RecurrenceType recType = recurrenceCombo.getValue();
        RecurrenceRule recRule = new RecurrenceRule(recType, time, null);

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
