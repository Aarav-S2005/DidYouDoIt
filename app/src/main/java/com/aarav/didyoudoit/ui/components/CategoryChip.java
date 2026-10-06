package com.aarav.didyoudoit.ui.components;

import com.aarav.didyoudoit.model.TaskCategory;
import com.aarav.didyoudoit.ui.theme.FontManager;
import com.aarav.didyoudoit.ui.theme.Theme;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.Objects;

/**
 * Reusable category chip representing task grouping with emoji icon.
 * Implements FR-11.
 */
public class CategoryChip extends HBox {

    private final TaskCategory category;
    private boolean selected;
    private final Label iconLabel;
    private final Label textLabel;

    public CategoryChip(TaskCategory category) {
        this(category, false);
    }

    public CategoryChip(TaskCategory category, boolean selectable) {
        super(Theme.SPACING_XS);
        this.category = Objects.requireNonNullElse(category, TaskCategory.OTHER);
        this.selected = false;

        setAlignment(Pos.CENTER);
        this.iconLabel = new Label("");
        this.iconLabel.setVisible(false);
        this.iconLabel.setManaged(false);
        this.textLabel = new Label(this.category.getDisplayName());
        this.textLabel.setFont(FontManager.getPrimaryFont(12));

        getChildren().add(textLabel);
        setupStyling(selectable);
    }

    private void setupStyling(boolean selectable) {
        getStyleClass().add("badge");
        updateSelectedState();

        if (selectable) {
            setCursor(javafx.scene.Cursor.HAND);
            setOnMouseClicked(e -> setSelected(!selected));
        }
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        updateSelectedState();
    }

    public boolean isSelected() {
        return selected;
    }

    public TaskCategory getCategory() {
        return category;
    }

    private void updateSelectedState() {
        getStyleClass().removeAll("badge-low", "badge-primary-chip", "badge-default-chip");
        if (selected) {
            getStyleClass().add("badge-primary-chip");
            textLabel.setTextFill(javafx.scene.paint.Color.WHITE);
            textLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
        } else {
            getStyleClass().add("badge-default-chip");
            textLabel.setTextFill(javafx.scene.paint.Color.web("#2B2D42"));
            textLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2B2D42;");
        }
    }
}
