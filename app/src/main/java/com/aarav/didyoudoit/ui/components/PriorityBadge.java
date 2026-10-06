package com.aarav.didyoudoit.ui.components;

import com.aarav.didyoudoit.model.Priority;
import com.aarav.didyoudoit.ui.theme.FontManager;
import com.aarav.didyoudoit.ui.theme.Theme;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.Objects;

/**
 * Reusable priority badge with color-coded dot indicator and rounded pill container.
 * Implements FR-10.
 */
public class PriorityBadge extends HBox {

    private final Priority priority;
    private final Label textLabel;

    public PriorityBadge(Priority priority) {
        super(Theme.SPACING_XS);
        this.priority = Objects.requireNonNullElse(priority, Priority.MEDIUM);

        setAlignment(Pos.CENTER);
        this.textLabel = new Label(this.priority.getDisplayName());
        this.textLabel.setFont(FontManager.getPrimaryFont(12));

        getChildren().add(textLabel);
        updateStyling();
    }

    private void updateStyling() {
        getStyleClass().clear();
        getStyleClass().addAll("badge", priority.getStyleClass());

        String colorHex = switch (priority) {
            case LOW -> "#0E6251";
            case MEDIUM -> "#856404";
            case HIGH -> "#C05600";
            case URGENT -> "#C0392B";
        };
        textLabel.setTextFill(Color.web(colorHex));
        textLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");
    }

    public Priority getPriority() {
        return priority;
    }
}
