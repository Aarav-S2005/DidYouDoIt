package com.aarav.didyoudoit.ui.components;

import com.aarav.didyoudoit.ui.theme.FontManager;
import com.aarav.didyoudoit.ui.theme.Theme;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
  * Minimalist, compact in-app toast notification card inspired by the modern Sonner toaster.
  * Sleek, low-profile footprint positioned unobtrusively at the bottom-right corner.
  * Implements FR-08, NFR-08.
  */
public class ToastNotificationCard extends HBox {

    public interface ToastActionListener {
        void onComplete();
        void onSnooze();
        void onDismiss();
    }

    private final PauseTransition autoDismissTimer;
    private boolean isDismissed = false;

    public ToastNotificationCard(String title, String body) {
        this(title, body, null);
    }

    public ToastNotificationCard(String title, String body, ToastActionListener listener) {
        super(Theme.SPACING_SM);

        getStyleClass().add("toast-sonner");
        setAlignment(Pos.CENTER_LEFT);
        setMaxWidth(340);
        setMinWidth(240);
        setPadding(new Insets(8, 12, 8, 12));

        // 1. Icon Glyph
        String iconGlyph = extractIcon(title);
        Label iconLabel = new Label(iconGlyph);
        iconLabel.setStyle("-fx-font-size: 14px; -fx-min-width: 18px;");

        // Text Container (Title + Subtitle)
        VBox textContainer = new VBox(1);
        textContainer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textContainer, Priority.ALWAYS);

        String cleanTitle = cleanTitleText(title);
        Label titleLabel = new Label(cleanTitle);
        titleLabel.setFont(FontManager.getPrimaryFont(12.5));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2B2D42;");
        titleLabel.setWrapText(false);
        textContainer.getChildren().add(titleLabel);

        if (body != null && !body.isBlank()) {
            Label bodyLabel = new Label(body);
            bodyLabel.setFont(FontManager.getPrimaryFont(11));
            bodyLabel.setStyle("-fx-text-fill: #6B7280;");
            bodyLabel.setWrapText(true);
            bodyLabel.setMaxWidth(230);
            textContainer.getChildren().add(bodyLabel);
        }

        getChildren().add(textContainer);

        if (listener != null) {
            Button doneBtn = createMiniActionButton("Done", e -> {
                dismiss(() -> listener.onComplete());
            });
            Button snoozeBtn = createMiniActionButton("Snooze", e -> {
                dismiss(() -> listener.onSnooze());
            });
            getChildren().addAll(doneBtn, snoozeBtn);
        }

        // Close text button
        Button closeBtn = new Button("Dismiss");
        closeBtn.setFont(FontManager.getPrimaryFont(10.5));
        closeBtn.getStyleClass().add("toast-close-btn");
        closeBtn.setOnAction(e -> dismiss(listener != null ? listener::onDismiss : null));
        getChildren().add(closeBtn);

        // 5. Auto-dismiss timer (3.5 seconds)
        this.autoDismissTimer = new PauseTransition(Duration.millis(3500));
        this.autoDismissTimer.setOnFinished(e -> dismiss(listener != null ? listener::onDismiss : null));

        // Pause auto-dismiss on mouse hover, resume on mouse exit
        setOnMouseEntered(e -> autoDismissTimer.pause());
        setOnMouseExited(e -> {
            if (!isDismissed) {
                autoDismissTimer.play();
            }
        });

        // Entrance animation: slide up slightly & fade in
        setOpacity(0.0);
        setTranslateY(12);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), this);
        fadeIn.setToValue(1.0);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(200), this);
        slideIn.setToY(0);

        fadeIn.play();
        slideIn.play();
        autoDismissTimer.play();
    }

    private Button createMiniActionButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.setFont(FontManager.getPrimaryFont(10.5));
        btn.setStyle("-fx-background-color: #F3EFEA; -fx-text-fill: #2B2D42; -fx-padding: 2px 7px; -fx-background-radius: 6px; -fx-cursor: hand; -fx-font-weight: bold;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #E27D60; -fx-text-fill: white; -fx-padding: 2px 7px; -fx-background-radius: 6px; -fx-cursor: hand; -fx-font-weight: bold;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #F3EFEA; -fx-text-fill: #2B2D42; -fx-padding: 2px 7px; -fx-background-radius: 6px; -fx-cursor: hand; -fx-font-weight: bold;"));
        btn.setOnAction(handler);
        return btn;
    }

    private String extractIcon(String title) {
        if (title.startsWith("✓")) return "✓";
        if (title.startsWith("⏱")) return "⏱";
        if (title.startsWith("🗑")) return "🗑";
        if (title.startsWith("⚡")) return "⚡";
        if (title.startsWith("ℹ")) return "ℹ";
        return "✨";
    }

    private String cleanTitleText(String title) {
        if (title == null) return "";
        return title.replaceFirst("^[✓⏱🗑⚡ℹ✨]\\s*", "").trim();
    }

    /**
     * Dismisses the toast smoothly with a fade and slide animation.
     */
    public void dismiss(Runnable onFinished) {
        if (isDismissed) return;
        isDismissed = true;
        autoDismissTimer.stop();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(180), this);
        fadeOut.setToValue(0.0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(180), this);
        slideOut.setToY(8);

        fadeOut.setOnFinished(e -> {
            if (getParent() instanceof Pane parentPane) {
                parentPane.getChildren().remove(this);
            }
            if (onFinished != null) {
                onFinished.run();
            }
        });

        fadeOut.play();
        slideOut.play();
    }
}
