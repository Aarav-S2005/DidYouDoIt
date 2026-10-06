package com.aarav.didyoudoit.ui.components;

import com.aarav.didyoudoit.ui.theme.FontManager;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.util.Duration;

/**
 * Reusable styled button component supporting multiple visual variants and hover micro-animations.
 */
public class AppButton extends Button {

    public enum Variant {
        PRIMARY("btn-primary"),
        SECONDARY("btn-secondary"),
        GHOST("btn-ghost"),
        DANGER("btn-danger");

        private final String cssClass;

        Variant(String cssClass) {
            this.cssClass = cssClass;
        }

        public String getCssClass() {
            return cssClass;
        }
    }

    private final Variant variant;

    public AppButton(String text, Variant variant) {
        super(text);
        this.variant = variant != null ? variant : Variant.PRIMARY;
        setupStyling();
        setupAnimations();
    }

    public AppButton(String text, Node graphic, Variant variant) {
        super(text, graphic);
        this.variant = variant != null ? variant : Variant.PRIMARY;
        setupStyling();
        setupAnimations();
    }

    public static AppButton primary(String text) {
        return new AppButton(text, Variant.PRIMARY);
    }

    public static AppButton secondary(String text) {
        return new AppButton(text, Variant.SECONDARY);
    }

    public static AppButton ghost(String text) {
        return new AppButton(text, Variant.GHOST);
    }

    public static AppButton danger(String text) {
        return new AppButton(text, Variant.DANGER);
    }

    private void setupStyling() {
        getStyleClass().addAll("button", variant.getCssClass());
        setFont(FontManager.getPrimaryFont(14));
        setFocusTraversable(true);
    }

    private void setupAnimations() {
        ScaleTransition hoverIn = new ScaleTransition(Duration.millis(120), this);
        hoverIn.setToX(1.02);
        hoverIn.setToY(1.02);

        ScaleTransition hoverOut = new ScaleTransition(Duration.millis(120), this);
        hoverOut.setToX(1.0);
        hoverOut.setToY(1.0);

        setOnMouseEntered(e -> {
            if (!isDisabled()) {
                hoverIn.playFromStart();
            }
        });

        setOnMouseExited(e -> {
            if (!isDisabled()) {
                hoverOut.playFromStart();
            }
        });
    }

    public Variant getVariant() {
        return variant;
    }
}
