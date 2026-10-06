package com.aarav.didyoudoit.view;

import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.model.TaskCategory;
import com.aarav.didyoudoit.ui.components.*;
import com.aarav.didyoudoit.ui.theme.FontManager;
import com.aarav.didyoudoit.ui.theme.Theme;
import com.aarav.didyoudoit.viewmodel.DashboardViewModel;
import com.aarav.didyoudoit.viewmodel.SettingsViewModel;
import com.aarav.didyoudoit.viewmodel.StatisticsViewModel;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Main application dashboard view driven reactively by {@link DashboardViewModel}.
 * Pure JavaFX implementation adhering strictly to the "No FXML" rule.
 * Implements FR-01, FR-02, FR-03, FR-08, FR-09, FR-11, FR-12, FR-14, FR-21.
 */
public class DashboardView extends BorderPane {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMM d");

    private final DashboardViewModel viewModel;
    private final StatisticsViewModel statisticsViewModel;
    private final StatisticsView statisticsView;
    private final SettingsViewModel settingsViewModel;
    private final SettingsView settingsView;
    private final StackPane toastContainer;
    private final VBox toastStack;

    private final VBox taskCardsContainer;
    private final Label sectionHeaderLabel;
    private final Label countBadgeLabel;
    private final StackPane centerContentStack;
    private final ProgressIndicator loadingIndicator;

    public DashboardView(DashboardViewModel viewModel, StackPane toastContainer) {
        this(viewModel, null, null, toastContainer);
    }

    public DashboardView(DashboardViewModel viewModel, StatisticsViewModel statisticsViewModel, StackPane toastContainer) {
        this(viewModel, statisticsViewModel, null, toastContainer);
    }

    public DashboardView(DashboardViewModel viewModel,
                         StatisticsViewModel statisticsViewModel,
                         SettingsViewModel settingsViewModel,
                         StackPane toastContainer) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel cannot be null");
        this.statisticsViewModel = statisticsViewModel;
        this.settingsViewModel = settingsViewModel;
        this.toastContainer = Objects.requireNonNull(toastContainer, "toastContainer cannot be null");
        this.statisticsView = (statisticsViewModel != null) ? new StatisticsView(statisticsViewModel, this::showToast) : null;
        this.settingsView = (settingsViewModel != null) ? new SettingsView(settingsViewModel, this::showToast) : null;

        // Sonner toast stack container at bottom-right corner
        this.toastStack = new VBox(Theme.SPACING_SM);
        this.toastStack.setAlignment(Pos.BOTTOM_RIGHT);
        this.toastStack.setPickOnBounds(false);
        this.toastStack.setMaxWidth(Region.USE_PREF_SIZE);
        this.toastStack.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(this.toastStack, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(this.toastStack, new Insets(0, 24, 24, 0));
        this.toastContainer.getChildren().add(this.toastStack);

        getStyleClass().add("dashboard-root");

        // 1. Sidebar (Left Panel)
        VBox sidebar = createSidebar();
        setLeft(sidebar);

        // 2. Top Bar (Upper Panel)
        HBox topBar = createTopBar();
        setTop(topBar);

        // 3. Center Workspace
        this.taskCardsContainer = new VBox(Theme.SPACING_SM);
        this.taskCardsContainer.setMaxWidth(Double.MAX_VALUE);

        this.sectionHeaderLabel = new Label("Today's Tasks");
        this.sectionHeaderLabel.setFont(FontManager.getPrimaryFont(18));
        this.sectionHeaderLabel.getStyleClass().add("section-header");

        this.countBadgeLabel = new Label("0 tasks");
        this.countBadgeLabel.getStyleClass().addAll("badge", "badge-default-chip");

        this.loadingIndicator = new ProgressIndicator();
        this.loadingIndicator.setMaxSize(40, 40);
        this.loadingIndicator.visibleProperty().bind(viewModel.isLoadingProperty());
        this.loadingIndicator.managedProperty().bind(viewModel.isLoadingProperty());

        this.centerContentStack = new StackPane();
        ScrollPane centerScrollPane = createCenterScrollPane();
        this.centerContentStack.getChildren().addAll(centerScrollPane, loadingIndicator);

        setCenter(centerContentStack);

        // Bindings & listeners
        setupBindings();

        // Initial load
        viewModel.reloadTasks();
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(Theme.SPACING_MD);
        sidebar.getStyleClass().add("sidebar");

        // Brand Title & Subtitle
        VBox brandBox = new VBox(2);
        Label brandTitle = new Label("DidYouDoIt?");
        brandTitle.setFont(FontManager.getHeroFont(40));
        brandTitle.getStyleClass().add("sidebar-brand-title");

        Label brandSub = new Label("Accountability & Habits");
        brandSub.setFont(FontManager.getAccentFont(14));
        brandSub.getStyleClass().add("sidebar-brand-sub");

        brandBox.getChildren().addAll(brandTitle, brandSub);

        // Navigation Items
        VBox navList = new VBox(Theme.SPACING_XS);

        Button navToday = createNavButton("Today's Tasks", DashboardViewModel.DashboardSection.TODAY);
        Button navOverdue = createNavButton("Overdue & Nagging", DashboardViewModel.DashboardSection.OVERDUE);
        Button navHabits = createNavButton("Daily Habits", DashboardViewModel.DashboardSection.HABITS);
        Button navStats = createNavButton("Progress & Stats", DashboardViewModel.DashboardSection.STATISTICS);
        Button navSettings = createNavButton("Settings", DashboardViewModel.DashboardSection.SETTINGS);
        Button navCompleted = createNavButton("Completed Tasks", DashboardViewModel.DashboardSection.COMPLETED);

        navList.getChildren().addAll(navToday, navOverdue, navHabits, navStats, navSettings, navCompleted);

        // Spacer pushing streak card to bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Streak Tracker Widget Card
        VBox streakCard = new VBox(Theme.SPACING_XS);
        streakCard.getStyleClass().add("quick-streak-card");

        Label streakTitle = new Label();
        streakTitle.setFont(FontManager.getPrimaryFont(13));
        streakTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #E27D60;");
        streakTitle.textProperty().bind(
                viewModel.bestStreakCountProperty().asString("🔥 Best Streak: %d days")
        );

        Label streakSub = new Label("Sarcastic Mentor is watching. Don't slip up today.");
        streakSub.setFont(FontManager.getAccentFont(12));
        streakSub.getStyleClass().add("caption-text");
        streakSub.setWrapText(true);

        streakCard.getChildren().addAll(streakTitle, streakSub);

        sidebar.getChildren().addAll(brandBox, navList, spacer, streakCard);
        return sidebar;
    }

    private Button createNavButton(String text, DashboardViewModel.DashboardSection section) {
        Button btn = new Button(text);
        btn.getStyleClass().add("nav-item");

        // Highlight active button
        viewModel.activeSectionProperty().addListener((obs, oldVal, newVal) -> {
            btn.getStyleClass().remove("nav-item-active");
            if (newVal == section) {
                btn.getStyleClass().add("nav-item-active");
            }
        });

        if (viewModel.activeSectionProperty().get() == section) {
            btn.getStyleClass().add("nav-item-active");
        }

        btn.setOnAction(e -> viewModel.activeSectionProperty().set(section));
        return btn;
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(Theme.SPACING_MD);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        // Left date header
        VBox dateBox = new VBox(2);
        Label dateLabel = new Label(LocalDate.now().format(DATE_FORMATTER));
        dateLabel.getStyleClass().add("top-date-label");

        Label quoteLabel = new Label("Get tasks done before the nagging begins.");
        quoteLabel.getStyleClass().add("top-quote-label");

        dateBox.getChildren().addAll(dateLabel, quoteLabel);

        // Center spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Search textfield with bidirectional binding
        TextField searchField = new TextField();
        searchField.setPromptText("Search tasks...");
        searchField.getStyleClass().add("input-text");
        searchField.setPrefWidth(220);
        searchField.textProperty().bindBidirectional(viewModel.searchQueryProperty());

        // Primary "+ Add Task" Button
        AppButton addTaskBtn = AppButton.primary("+ Add Task");
        addTaskBtn.setOnAction(e -> openCreateTaskDialog());

        topBar.getChildren().addAll(dateBox, spacer, searchField, addTaskBtn);
        return topBar;
    }

    private ScrollPane createCenterScrollPane() {
        VBox contentPane = new VBox(Theme.SPACING_LG);
        contentPane.getStyleClass().add("main-content-pane");
        contentPane.setMaxWidth(Double.MAX_VALUE);

        // 1. Category Filter Chips (Fluid FlowPane)
        VBox filterSection = new VBox(Theme.SPACING_SM);
        Label filterHeader = new Label("Filter by Category:");
        filterHeader.setFont(FontManager.getPrimaryFont(13));
        filterHeader.getStyleClass().add("caption-text");

        FlowPane chipsFlow = createCategoryFilterChips();
        filterSection.getChildren().addAll(filterHeader, chipsFlow);

        // 2. Section Header row
        HBox sectionHeaderRow = new HBox(Theme.SPACING_SM);
        sectionHeaderRow.setAlignment(Pos.CENTER_LEFT);
        sectionHeaderRow.getChildren().addAll(sectionHeaderLabel, countBadgeLabel);

        contentPane.getChildren().addAll(filterSection, sectionHeaderRow, taskCardsContainer);

        ScrollPane scrollPane = new ScrollPane(contentPane);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");
        return scrollPane;
    }

    private FlowPane createCategoryFilterChips() {
        FlowPane flow = new FlowPane(Theme.SPACING_SM, Theme.SPACING_SM);
        flow.setAlignment(Pos.CENTER_LEFT);
        flow.setMaxWidth(Double.MAX_VALUE);

        CategoryChip allChip = new CategoryChip(TaskCategory.OTHER, false);
        allChip.setCursor(javafx.scene.Cursor.HAND);
        allChip.setSelected(viewModel.selectedCategoryFilterProperty().get() == null);

        allChip.setOnMouseClicked(e -> {
            viewModel.selectedCategoryFilterProperty().set(null);
            updateChipsSelection(flow, null);
        });
        flow.getChildren().add(allChip);

        for (TaskCategory cat : TaskCategory.values()) {
            if (cat != TaskCategory.OTHER) {
                CategoryChip chip = new CategoryChip(cat, false);
                chip.setCursor(javafx.scene.Cursor.HAND);
                chip.setOnMouseClicked(e -> {
                    if (viewModel.selectedCategoryFilterProperty().get() == cat) {
                        viewModel.selectedCategoryFilterProperty().set(null);
                        updateChipsSelection(flow, null);
                    } else {
                        viewModel.selectedCategoryFilterProperty().set(cat);
                        updateChipsSelection(flow, cat);
                    }
                });
                flow.getChildren().add(chip);
            }
        }

        return flow;
    }

    private void updateChipsSelection(FlowPane flow, TaskCategory selected) {
        for (javafx.scene.Node node : flow.getChildren()) {
            if (node instanceof CategoryChip chip) {
                if (selected == null) {
                    chip.setSelected(chip.getCategory() == TaskCategory.OTHER);
                } else {
                    chip.setSelected(chip.getCategory() == selected);
                }
            }
        }
    }

    private void setupBindings() {
        // Update section title and center view when navigation changes
        viewModel.activeSectionProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == DashboardViewModel.DashboardSection.STATISTICS && statisticsView != null) {
                setCenter(statisticsView);
                statisticsView.refresh();
            } else if (newVal == DashboardViewModel.DashboardSection.SETTINGS && settingsView != null) {
                setCenter(settingsView);
            } else {
                setCenter(centerContentStack);
                sectionHeaderLabel.setText(newVal.getTitle());
            }
        });

        // Re-render task cards whenever displayed list changes
        viewModel.getDisplayedTasks().addListener((ListChangeListener<Task>) c -> {
            renderTaskCards();
        });
    }

    private void renderTaskCards() {
        taskCardsContainer.getChildren().clear();

        var tasks = viewModel.getDisplayedTasks();
        countBadgeLabel.setText(tasks.size() + (tasks.size() == 1 ? " task" : " tasks"));

        if (tasks.isEmpty()) {
            EmptyStateView emptyState = switch (viewModel.activeSectionProperty().get()) {
                case TODAY -> EmptyStateView.noTasks(this::openCreateTaskDialog);
                case OVERDUE -> EmptyStateView.noOverdue();
                default -> new EmptyStateView("📋", "No tasks found", "Try clearing your search query or filters.", "+ Add Task", this::openCreateTaskDialog);
            };
            taskCardsContainer.getChildren().add(emptyState);
            return;
        }

        Window currentWindow = getScene() != null ? getScene().getWindow() : null;

        for (Task task : tasks) {
            TaskCardView card = new TaskCardView(task, new TaskCardView.TaskActionListener() {
                @Override
                public void onToggleComplete(Task t) {
                    viewModel.toggleComplete(t).thenRun(() -> {
                        Platform.runLater(() -> showToast("✓ " + t.getTitle(), "Task marked as " + (t.getStatus() == com.aarav.didyoudoit.model.TaskStatus.COMPLETED ? "completed!" : "pending.")));
                    });
                }

                @Override
                public void onSnooze(Task t) {
                    viewModel.snoozeTask(t, 15).thenRun(() -> {
                        Platform.runLater(() -> showToast("⏱ Snoozed 15m", "'" + t.getTitle() + "' postponed for 15 minutes."));
                    });
                }

                @Override
                public void onEdit(Task t) {
                    TaskDialog.show(currentWindow, t).ifPresent(viewModel::saveTask);
                }

                @Override
                public void onDelete(Task t) {
                    boolean confirmed = ConfirmationDialog.show(
                            currentWindow,
                            "Delete Task?",
                            "Are you sure you want to delete '" + t.getTitle() + "'?",
                            "Delete",
                            true
                    );
                    if (confirmed) {
                        viewModel.deleteTask(t).thenRun(() -> {
                            Platform.runLater(() -> showToast("🗑 Deleted", "'" + t.getTitle() + "' moved to trash."));
                        });
                    }
                }
            });

            taskCardsContainer.getChildren().add(card);
        }
    }

    private void openCreateTaskDialog() {
        Window window = getScene() != null ? getScene().getWindow() : null;
        TaskDialog.show(window, null).ifPresent(viewModel::saveTask);
    }

    public void displayToast(ToastNotificationCard toast) {
        if (toast != null && !toastStack.getChildren().contains(toast)) {
            toastStack.getChildren().add(toast);
        }
    }

    private void showToast(String title, String body) {
        displayToast(new ToastNotificationCard(title, body));
    }
}
