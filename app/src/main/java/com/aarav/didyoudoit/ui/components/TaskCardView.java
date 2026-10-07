package com.aarav.didyoudoit.ui.components;

import com.aarav.didyoudoit.model.EscalationLevel;
import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.model.TaskStatus;
import com.aarav.didyoudoit.ui.theme.FontManager;
import com.aarav.didyoudoit.ui.theme.Theme;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Reusable visual task card representing a task item with interactive controls,
 * status badges, escalation markers, focus timer countdown, and responsive fluid layout.
 * Adapts seamlessly across screen sizes from narrow half-screen snapping to large desktop displays.
 * Implements FR-01, FR-02, FR-09, FR-10, FR-11, FR-12, NFR-08, NFR-13.
 */
public class TaskCardView extends HBox {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");

    public interface TaskActionListener {
        void onToggleComplete(Task task);
        void onSnooze(Task task);
        void onEdit(Task task);
        void onDelete(Task task);
        default void onToggleTimer(Task task) {}
        default void onTimerFinished(Task task) {}
    }

    private final Task task;
    private final TaskActionListener listener;
    private final Button checkButton;
    private final Label titleLabel;
    private final Label descriptionLabel;
    private final FlowPane badgeRow;
    private Timeline timerTimeline;

    public TaskCardView(Task task, TaskActionListener listener) {
        super(Theme.SPACING_MD);
        this.task = Objects.requireNonNull(task, "task cannot be null");
        this.listener = listener;

        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(Theme.SPACING_MD));
        getStyleClass().add("card");
        setEffect(Theme.SHADOW_SOFT);
        setMaxWidth(Double.MAX_VALUE);

        // 1. Completion Checkbox / Toggle Button
        this.checkButton = createCheckButton();

        // 2. Center Content Box (Title, Description, Badges)
        VBox centerBox = new VBox(Theme.SPACING_XS);
        HBox.setHgrow(centerBox, Priority.ALWAYS);
        centerBox.setMinWidth(120);

        this.titleLabel = new Label(task.getTitle());
        this.titleLabel.setFont(FontManager.getPrimaryFont(15));
        this.titleLabel.getStyleClass().add("section-header");
        this.titleLabel.setWrapText(true);
        this.titleLabel.setMaxWidth(Double.MAX_VALUE);

        this.descriptionLabel = new Label(task.getDescription());
        this.descriptionLabel.setFont(FontManager.getPrimaryFont(12));
        this.descriptionLabel.getStyleClass().add("caption-text");
        this.descriptionLabel.setWrapText(true);
        this.descriptionLabel.setMaxWidth(Double.MAX_VALUE);
        if (task.getDescription() == null || task.getDescription().isBlank()) {
            this.descriptionLabel.setVisible(false);
            this.descriptionLabel.setManaged(false);
        }

        // FlowPane for badges to wrap cleanly on narrow screens (e.g. half-screen or small 13" laptop)
        this.badgeRow = createBadgeRow();
        centerBox.getChildren().addAll(titleLabel, descriptionLabel, badgeRow);

        // 3. Right Action Buttons Box
        HBox actionsBox = createActionsBox();

        getChildren().addAll(checkButton, centerBox, actionsBox);

        setupCardState();
        setupHoverAnimation();
    }

    private Button createCheckButton() {
        boolean isCompleted = task.getStatus() == TaskStatus.COMPLETED;
        Button btn = new Button(isCompleted ? "✓" : "");
        btn.getStyleClass().add("check-toggle-btn");
        if (isCompleted) {
            btn.getStyleClass().add("check-toggle-completed");
        }
        btn.setMinSize(28, 28);
        btn.setMaxSize(28, 28);
        btn.setOnAction(e -> {
            boolean willBeCompleted = task.getStatus() != TaskStatus.COMPLETED;
            if (willBeCompleted) {
                btn.setText("✓");
                if (!btn.getStyleClass().contains("check-toggle-completed")) {
                    btn.getStyleClass().add("check-toggle-completed");
                }
                titleLabel.setStyle("-fx-strikethrough: true; -fx-opacity: 0.6;");
                if (timerTimeline != null) {
                    timerTimeline.stop();
                }
                task.setTimerActive(false);
            } else {
                btn.setText("");
                btn.getStyleClass().remove("check-toggle-completed");
                titleLabel.setStyle("");
            }
            if (listener != null) {
                listener.onToggleComplete(task);
            }
        });
        return btn;
    }

    private FlowPane createBadgeRow() {
        FlowPane flow = new FlowPane(Theme.SPACING_SM, Theme.SPACING_XS);
        flow.setAlignment(Pos.CENTER_LEFT);
        flow.setMaxWidth(Double.MAX_VALUE);

        // Priority Badge
        flow.getChildren().add(new PriorityBadge(task.getPriority()));

        // Category Chip
        flow.getChildren().add(new CategoryChip(task.getCategory()));

        // Recurrence indicator if habit
        if (task.isTemplate() || task.getParentTemplateId() != null) {
            Label habitLabel = new Label("Daily Habit");
            habitLabel.setFont(FontManager.getPrimaryFont(11));
            habitLabel.setTextFill(javafx.scene.paint.Color.web("#2B2D42"));
            habitLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2B2D42;");
            habitLabel.getStyleClass().addAll("badge", "badge-default-chip");
            flow.getChildren().add(habitLabel);
        }

        // Focus duration badge if configured
        if (task.hasDuration()) {
            String durText = task.getDurationMinutes() >= 60
                    ? String.format("%.1fh focus", task.getDurationMinutes() / 60.0).replace(".0h", "h")
                    : task.getDurationMinutes() + "m focus";
            Label durBadge = new Label("⏱ " + durText);
            durBadge.setFont(FontManager.getPrimaryFont(11));
            durBadge.setTextFill(javafx.scene.paint.Color.web("#1D3557"));
            durBadge.setStyle("-fx-font-weight: bold; -fx-text-fill: #1D3557;");
            durBadge.getStyleClass().addAll("badge", "badge-duration");
            flow.getChildren().add(durBadge);
        }

        // Due time label
        if (task.getDueDateTime() != null) {
            String dueText = formatDueText(task.getDueDateTime());
            Label dueLabel = new Label(dueText);
            dueLabel.setFont(FontManager.getPrimaryFont(11));
            dueLabel.setTextFill(javafx.scene.paint.Color.web("#2B3A4A"));
            dueLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2B3A4A;");
            dueLabel.getStyleClass().addAll("badge", "badge-time");
            flow.getChildren().add(dueLabel);
        }

        // Nag count & escalation level badge if active
        if (task.getNagCount() > 0) {
            String nagText = task.getNagCount() + " " + (task.getNagCount() == 1 ? "nudge" : "nudges");
            Label nagLabel = new Label(nagText);
            nagLabel.setFont(FontManager.getPrimaryFont(11));
            nagLabel.setTextFill(javafx.scene.paint.Color.web("#C0392B"));
            nagLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #C0392B;");
            nagLabel.getStyleClass().addAll("badge", "badge-urgent");
            flow.getChildren().add(nagLabel);
        }

        return flow;
    }

    private String formatDueText(LocalDateTime due) {
        if (task.getStatus() == TaskStatus.POSTPONED && task.getPostponedUntil() != null) {
            return "Snoozed until " + task.getPostponedUntil().format(TIME_FORMAT);
        }
        return "Due " + due.format(TIME_FORMAT);
    }

    private HBox createActionsBox() {
        HBox box = new HBox(Theme.SPACING_XS);
        box.setAlignment(Pos.CENTER_RIGHT);

        if (task.getStatus() != TaskStatus.COMPLETED) {
            if (task.hasDuration()) {
                Button timerBtn = createTimerButton();
                box.getChildren().add(timerBtn);
            }

            Button snoozeBtn = createTextActionButton("Snooze", e -> {
                if (listener != null) listener.onSnooze(task);
            });
            box.getChildren().add(snoozeBtn);
        }

        Button editBtn = createTextActionButton("Edit", e -> {
            if (listener != null) listener.onEdit(task);
        });

        Button deleteBtn = createTextActionButton("Delete", e -> {
            if (listener != null) listener.onDelete(task);
        });

        box.getChildren().addAll(editBtn, deleteBtn);
        return box;
    }

    private Button createTimerButton() {
        Button btn = new Button();
        updateTimerButtonDisplay(btn);

        btn.setOnAction(e -> {
            boolean wasActive = task.isTimerActive();
            if (wasActive) {
                task.setTimerActive(false);
                if (timerTimeline != null) {
                    timerTimeline.stop();
                }
                updateTimerButtonDisplay(btn);
            } else {
                if (task.getTimerRemainingSeconds() <= 0) {
                    task.setTimerRemainingSeconds(task.getDurationMinutes() * 60);
                }
                task.setTimerActive(true);
                startTimeline(btn);
                updateTimerButtonDisplay(btn);
            }

            if (listener != null) {
                listener.onToggleTimer(task);
            }
        });

        if (task.isTimerActive() && task.getTimerRemainingSeconds() > 0) {
            startTimeline(btn);
        }

        return btn;
    }

    private void startTimeline(Button btn) {
        if (timerTimeline != null) {
            timerTimeline.stop();
        }
        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            int rem = task.getTimerRemainingSeconds() - 1;
            if (rem <= 0) {
                task.setTimerRemainingSeconds(0);
                task.setTimerActive(false);
                if (timerTimeline != null) {
                    timerTimeline.stop();
                }
                updateTimerButtonDisplay(btn);
                if (listener != null) {
                    listener.onTimerFinished(task);
                }
            } else {
                task.setTimerRemainingSeconds(rem);
                updateTimerButtonDisplay(btn);
            }
        }));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.play();
    }

    private void updateTimerButtonDisplay(Button btn) {
        btn.getStyleClass().removeAll("btn-timer", "btn-timer-active");
        int rem = task.getTimerRemainingSeconds();
        int mins = rem / 60;
        int secs = rem % 60;
        String formatted = String.format("%02d:%02d", mins, secs);

        if (task.isTimerActive()) {
            btn.setText("⏸ " + formatted);
            btn.getStyleClass().add("btn-timer-active");
        } else {
            if (rem <= 0) {
                btn.setText("🔄 Restart (" + task.getDurationMinutes() + "m)");
            } else if (rem == task.getDurationMinutes() * 60) {
                btn.setText("▶ Start (" + task.getDurationMinutes() + "m)");
            } else {
                btn.setText("▶ Resume (" + formatted + ")");
            }
            btn.getStyleClass().add("btn-timer");
        }
    }

    private Button createTextActionButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.getStyleClass().add("btn-card-action");
        btn.setOnAction(handler);
        return btn;
    }

    private void setupCardState() {
        if (task.getStatus() == TaskStatus.COMPLETED) {
            titleLabel.setStyle("-fx-strikethrough: true; -fx-opacity: 0.6;");
            checkButton.getStyleClass().add("check-toggle-completed");
            setOpacity(0.75);
        } else if (task.isOverdue(LocalDateTime.now()) || task.getEscalationLevel() == EscalationLevel.CRITICAL) {
            getStyleClass().add("card-overdue");
        }
    }

    private void setupHoverAnimation() {
        TranslateTransition moveUp = new TranslateTransition(Duration.millis(120), this);
        moveUp.setToY(-2);

        TranslateTransition moveDown = new TranslateTransition(Duration.millis(120), this);
        moveDown.setToY(0);

        setOnMouseEntered(e -> {
            setEffect(Theme.SHADOW_HOVER);
            moveUp.playFromStart();
        });

        setOnMouseExited(e -> {
            setEffect(Theme.SHADOW_SOFT);
            moveDown.playFromStart();
        });
    }

    public Task getTask() {
        return task;
    }
}
