package com.aarav.didyoudoit.view;

import com.aarav.didyoudoit.model.StreakInfo;
import com.aarav.didyoudoit.model.TaskHistory;
import com.aarav.didyoudoit.ui.components.AppButton;
import com.aarav.didyoudoit.ui.components.CategoryChip;
import com.aarav.didyoudoit.ui.components.EmptyStateView;
import com.aarav.didyoudoit.ui.components.PriorityBadge;
import com.aarav.didyoudoit.ui.theme.FontManager;
import com.aarav.didyoudoit.ui.theme.Theme;
import com.aarav.didyoudoit.viewmodel.StatisticsViewModel;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Dedicated visual dashboard for habit streaks, completion metrics,
 * accountability alerts, and task completion history log.
 * Pure JavaFX implementation adhering strictly to "No FXML" and responsive UI rules.
 * Implements FR-02, FR-13, FR-14, FR-22, NFR-08.
 */
public class StatisticsView extends ScrollPane {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    private final StatisticsViewModel viewModel;
    private final BiConsumer<String, String> toastNotifier;

    private final VBox contentBox;
    private final HBox alertBanner;
    private final Label alertTextLabel;
    private final VBox streaksContainer;
    private final VBox historyContainer;

    public StatisticsView(StatisticsViewModel viewModel, BiConsumer<String, String> toastNotifier) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel cannot be null");
        this.toastNotifier = toastNotifier;

        setFitToWidth(true);
        getStyleClass().add("scroll-pane");

        this.streaksContainer = new VBox(Theme.SPACING_SM);
        this.streaksContainer.setMaxWidth(Double.MAX_VALUE);

        this.historyContainer = new VBox(Theme.SPACING_SM);
        this.historyContainer.setMaxWidth(Double.MAX_VALUE);

        this.contentBox = new VBox(Theme.SPACING_LG);
        this.contentBox.setPadding(new Insets(Theme.SPACING_LG));
        this.contentBox.getStyleClass().add("main-content-pane");
        this.contentBox.setMaxWidth(Double.MAX_VALUE);

        // 1. Header Banner
        VBox headerBox = createHeaderBox();

        // 2. Accountability Alert Banner (FR-22)
        this.alertTextLabel = new Label();
        this.alertTextLabel.setFont(FontManager.getPrimaryFont(13));
        this.alertTextLabel.setStyle("-fx-text-fill: #856404; -fx-font-weight: bold;");
        this.alertTextLabel.setWrapText(true);

        this.alertBanner = new HBox(Theme.SPACING_SM);
        this.alertBanner.setAlignment(Pos.CENTER_LEFT);
        this.alertBanner.getStyleClass().add("alert-banner");
        this.alertBanner.getChildren().add(alertTextLabel);
        HBox.setHgrow(alertTextLabel, Priority.ALWAYS);

        // Bind alert banner visibility
        this.alertBanner.visibleProperty().bind(viewModel.missedHabitsAlertProperty().isNotEmpty());
        this.alertBanner.managedProperty().bind(viewModel.missedHabitsAlertProperty().isNotEmpty());
        this.alertTextLabel.textProperty().bind(viewModel.missedHabitsAlertProperty());

        // 3. KPI Metric Cards Row
        FlowPane kpiRow = createKpiCardsRow();

        // 4. Habit Streaks Section (FR-02, FR-13)
        VBox streaksSection = createStreaksSection();

        // 5. Completion History Section (FR-14)
        VBox historySection = createHistorySection();

        this.contentBox.getChildren().addAll(headerBox, alertBanner, kpiRow, streaksSection, historySection);
        setContent(contentBox);

        // Listen for data updates to re-render dynamic streak and history rows
        setupListeners();
    }

    private VBox createHeaderBox() {
        VBox headerBox = new VBox(4);
        Label titleLabel = new Label("Progress & Analytics");
        titleLabel.setFont(FontManager.getPrimaryFont(24));
        titleLabel.getStyleClass().add("section-header");

        Label subtitleLabel = new Label("Consistency metrics, habit streaks momentum, and activity history.");
        subtitleLabel.setFont(FontManager.getAccentFont(14));
        subtitleLabel.getStyleClass().add("caption-text");

        headerBox.getChildren().addAll(titleLabel, subtitleLabel);
        return headerBox;
    }

    private FlowPane createKpiCardsRow() {
        FlowPane flow = new FlowPane(Theme.SPACING_MD, Theme.SPACING_MD);
        flow.setAlignment(Pos.CENTER_LEFT);
        flow.setMaxWidth(Double.MAX_VALUE);

        // Card 1: 7-Day Completion Rate
        VBox card1 = createKpiCard("7-Day Rate", viewModel.completionRate7DaysProperty(), "Last 7 days");

        // Card 2: 30-Day Completion Rate
        VBox card2 = createKpiCard("30-Day Rate", viewModel.completionRate30DaysProperty(), "Last 30 days");

        // Card 3: Active Habit Streaks
        VBox card3 = createKpiCardNumeric("Active Streaks", viewModel.activeStreaksCountProperty(), "Building habits");

        // Card 4: Total Completed All-Time
        VBox card4 = createKpiCardNumeric("Total Completed", viewModel.totalCompletedProperty(), "All-time productivity");

        flow.getChildren().addAll(card1, card2, card3, card4);
        return flow;
    }

    private VBox createKpiCard(String label, javafx.beans.property.StringProperty valProp, String subtitle) {
        VBox card = new VBox(4);
        card.getStyleClass().add("kpi-card");
        card.setMinWidth(170);
        card.setPrefWidth(210);

        Label lbl = new Label(label);
        lbl.getStyleClass().add("kpi-lbl");

        Label val = new Label();
        val.getStyleClass().add("kpi-val");
        val.textProperty().bind(valProp);

        Label sub = new Label(subtitle);
        sub.setFont(FontManager.getPrimaryFont(11));
        sub.setStyle("-fx-text-fill: #8D99AE;");

        card.getChildren().addAll(lbl, val, sub);
        return card;
    }

    private VBox createKpiCardNumeric(String label, javafx.beans.property.IntegerProperty valProp, String subtitle) {
        VBox card = new VBox(4);
        card.getStyleClass().add("kpi-card");
        card.setMinWidth(170);
        card.setPrefWidth(210);

        Label lbl = new Label(label);
        lbl.getStyleClass().add("kpi-lbl");

        Label val = new Label();
        val.getStyleClass().add("kpi-val");
        val.textProperty().bind(valProp.asString());

        Label sub = new Label(subtitle);
        sub.setFont(FontManager.getPrimaryFont(11));
        sub.setStyle("-fx-text-fill: #8D99AE;");

        card.getChildren().addAll(lbl, val, sub);
        return card;
    }

    private VBox createStreaksSection() {
        VBox section = new VBox(Theme.SPACING_SM);
        Label header = new Label("Habit Streaks Momentum");
        header.setFont(FontManager.getPrimaryFont(18));
        header.getStyleClass().add("section-header");

        section.getChildren().addAll(header, streaksContainer);
        return section;
    }

    private VBox createHistorySection() {
        VBox section = new VBox(Theme.SPACING_SM);
        Label header = new Label("Recent Activity History Log");
        header.setFont(FontManager.getPrimaryFont(18));
        header.getStyleClass().add("section-header");

        Label subHeader = new Label("Completed tasks recorded in history. Click 'Undo' to reopen any task.");
        subHeader.setFont(FontManager.getPrimaryFont(12));
        subHeader.getStyleClass().add("caption-text");

        section.getChildren().addAll(header, subHeader, historyContainer);
        return section;
    }

    private void setupListeners() {
        viewModel.getHabitStreaksList().addListener((ListChangeListener<StreakInfo>) c -> renderStreaks());
        viewModel.getHistoryLogList().addListener((ListChangeListener<TaskHistory>) c -> renderHistory());
    }

    private void renderStreaks() {
        streaksContainer.getChildren().clear();

        var streaks = viewModel.getHabitStreaksList();
        if (streaks.isEmpty()) {
            VBox emptyBox = new VBox(Theme.SPACING_SM);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(Theme.SPACING_LG));
            emptyBox.getStyleClass().add("empty-state-container");

            Label msg = new Label("No habit streaks recorded yet.");
            msg.setFont(FontManager.getPrimaryFont(14));
            msg.setStyle("-fx-text-fill: #6B7280;");

            Label hint = new Label("Create a recurring habit (e.g. Daily Exercise, Drink Water) to start building streaks!");
            hint.setFont(FontManager.getAccentFont(13));
            hint.setStyle("-fx-text-fill: #8D99AE;");

            emptyBox.getChildren().addAll(msg, hint);
            streaksContainer.getChildren().add(emptyBox);
            return;
        }

        for (StreakInfo streak : streaks) {
            HBox card = new HBox(Theme.SPACING_MD);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(Theme.SPACING_MD));
            card.getStyleClass().add("card");
            card.setMaxWidth(Double.MAX_VALUE);

            VBox infoBox = new VBox(2);
            Label title = new Label(streak.getTaskTitle());
            title.setFont(FontManager.getPrimaryFont(15));
            title.setStyle("-fx-font-weight: bold; -fx-text-fill: #2B2D42;");

            String lastDateStr = streak.getLastCompletedDate() != null ? "Last done: " + streak.getLastCompletedDate() : "Just started";
            Label meta = new Label(lastDateStr);
            meta.setFont(FontManager.getPrimaryFont(11));
            meta.setStyle("-fx-text-fill: #8D99AE;");
            infoBox.getChildren().addAll(title, meta);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            // Current streak badge
            Label currentStreakBadge = new Label(streak.getCurrentStreak() + " day streak");
            currentStreakBadge.setFont(FontManager.getPrimaryFont(12));
            currentStreakBadge.getStyleClass().addAll("badge", "badge-urgent");

            // Best streak badge
            Label bestStreakBadge = new Label("Best: " + streak.getBestStreak() + " days");
            bestStreakBadge.setFont(FontManager.getPrimaryFont(11));
            bestStreakBadge.getStyleClass().addAll("badge", "badge-default-chip");

            card.getChildren().addAll(infoBox, currentStreakBadge, bestStreakBadge);
            streaksContainer.getChildren().add(card);
        }
    }

    private void renderHistory() {
        historyContainer.getChildren().clear();

        var historyList = viewModel.getHistoryLogList();
        if (historyList.isEmpty()) {
            VBox emptyBox = new VBox(Theme.SPACING_SM);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(Theme.SPACING_LG));
            emptyBox.getStyleClass().add("empty-state-container");

            Label msg = new Label("No completed tasks in history yet.");
            msg.setFont(FontManager.getPrimaryFont(14));
            msg.setStyle("-fx-text-fill: #6B7280;");

            emptyBox.getChildren().add(msg);
            historyContainer.getChildren().add(emptyBox);
            return;
        }

        for (TaskHistory item : historyList) {
            HBox row = new HBox(Theme.SPACING_SM);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 14, 10, 14));
            row.getStyleClass().add("card");
            row.setMaxWidth(Double.MAX_VALUE);

            Label checkIcon = new Label("✓");
            checkIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: #85B79D; -fx-font-weight: bold;");

            VBox infoBox = new VBox(2);
            Label titleLabel = new Label(item.getTaskTitle());
            titleLabel.setFont(FontManager.getPrimaryFont(13));
            titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2B2D42;");

            String timeStr = "Completed " + item.getCompletedAt().format(DATE_TIME_FORMATTER);
            Label timeLabel = new Label(timeStr);
            timeLabel.setFont(FontManager.getPrimaryFont(11));
            timeLabel.setStyle("-fx-text-fill: #8D99AE;");

            infoBox.getChildren().addAll(titleLabel, timeLabel);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            // Category & Priority
            CategoryChip catChip = new CategoryChip(item.getCategory());
            PriorityBadge priBadge = new PriorityBadge(item.getPriority());

            // Overdue indicator
            Label overdueTag = null;
            if (item.isWasOverdue()) {
                overdueTag = new Label("Overdue");
                overdueTag.setFont(FontManager.getPrimaryFont(10));
                overdueTag.getStyleClass().addAll("badge", "badge-urgent");
            }

            // Undo Button (FR-14)
            AppButton undoBtn = AppButton.ghost("Undo");
            undoBtn.setFont(FontManager.getPrimaryFont(11));
            undoBtn.setOnAction(e -> {
                viewModel.undoCompletion(item).thenRun(() -> {
                    if (toastNotifier != null) {
                        Platform.runLater(() -> toastNotifier.accept("↩ Task Reopened", "'" + item.getTaskTitle() + "' moved back to pending."));
                    }
                });
            });

            row.getChildren().addAll(checkIcon, infoBox, catChip, priBadge);
            if (overdueTag != null) {
                row.getChildren().add(overdueTag);
            }
            row.getChildren().add(undoBtn);

            historyContainer.getChildren().add(row);
        }
    }

    /**
     * Refreshes view statistics from the database.
     */
    public void refresh() {
        viewModel.reloadStats();
    }
}
