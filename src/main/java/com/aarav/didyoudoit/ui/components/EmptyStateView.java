package com.aarav.didyoudoit.ui.components;

import com.aarav.didyoudoit.ui.theme.FontManager;
import com.aarav.didyoudoit.ui.theme.Theme;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Reusable friendly empty state view with Indie Flower typography and responsive fluid wrapping.
 */
public class EmptyStateView extends VBox {

    private final Label iconLabel;
    private final Label titleLabel;
    private final Label descriptionLabel;
    private final AppButton actionButton;

    public EmptyStateView(String emoji, String title, String description, String actionButtonText, Runnable onAction) {
        super(Theme.SPACING_SM);
        setAlignment(Pos.CENTER);
        setPadding(new javafx.geometry.Insets(Theme.SPACING_XL));
        getStyleClass().add("empty-state-container");
        setMaxWidth(Double.MAX_VALUE);

        if (emoji != null && !emoji.isBlank()) {
            this.iconLabel = new Label(emoji);
            this.iconLabel.setStyle("-fx-font-size: 32px;");
            getChildren().add(iconLabel);
        } else {
            this.iconLabel = null;
        }

        this.titleLabel = new Label(Objects.requireNonNullElse(title, "Nothing here yet!"));
        this.titleLabel.setFont(FontManager.getAccentFont(22));
        this.titleLabel.getStyleClass().add("font-accent");
        this.titleLabel.setWrapText(true);
        this.titleLabel.setAlignment(Pos.CENTER);

        this.descriptionLabel = new Label(Objects.requireNonNullElse(description, ""));
        this.descriptionLabel.setFont(FontManager.getPrimaryFont(14));
        this.descriptionLabel.getStyleClass().add("caption-text");
        this.descriptionLabel.setWrapText(true);
        this.descriptionLabel.setMaxWidth(Double.MAX_VALUE);
        this.descriptionLabel.setAlignment(Pos.CENTER);

        getChildren().addAll(titleLabel, descriptionLabel);

        if (actionButtonText != null && !actionButtonText.isBlank() && onAction != null) {
            this.actionButton = AppButton.primary(actionButtonText);
            this.actionButton.setOnAction(e -> onAction.run());
            VBox.setMargin(this.actionButton, new javafx.geometry.Insets(Theme.SPACING_SM, 0, 0, 0));
            getChildren().add(actionButton);
        } else {
            this.actionButton = null;
        }
    }

    public static EmptyStateView noTasks(Runnable onAddTask) {
        return new EmptyStateView(
                null,
                "All done for today!",
                "You have no pending tasks. Relax, or set up another goal to keep your momentum going.",
                "+ Add New Task",
                onAddTask
        );
    }

    public static EmptyStateView noOverdue() {
        return new EmptyStateView(
                null,
                "Zero overdue tasks!",
                "You're completely on schedule. No nagging necessary right now.",
                null,
                null
        );
    }
}
