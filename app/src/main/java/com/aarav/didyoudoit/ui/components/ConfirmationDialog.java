package com.aarav.didyoudoit.ui.components;

import com.aarav.didyoudoit.ui.theme.FontManager;
import com.aarav.didyoudoit.ui.theme.Theme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reusable modal confirmation dialog styled in harmony with the application theme
 * and responsive across window sizes.
 */
public final class ConfirmationDialog {

    private ConfirmationDialog() {}

    /**
     * Shows a modal confirmation dialog and returns true if confirmed.
     */
    public static boolean show(Window owner, String title, String message, String confirmText, boolean isDanger) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialogStage.initOwner(owner);
        }
        dialogStage.initStyle(StageStyle.UNDECORATED);

        AtomicBoolean result = new AtomicBoolean(false);

        VBox content = new VBox(Theme.SPACING_MD);
        content.getStyleClass().add("card-elevated");
        content.setPadding(new Insets(Theme.SPACING_LG));
        content.setMinWidth(300);
        content.setMaxWidth(460);

        Label titleLabel = new Label(title);
        titleLabel.setFont(FontManager.getPrimaryFont(18));
        titleLabel.getStyleClass().add("section-header");
        titleLabel.setWrapText(true);

        Label messageLabel = new Label(message);
        messageLabel.setFont(FontManager.getPrimaryFont(14));
        messageLabel.getStyleClass().add("body-text");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(Double.MAX_VALUE);

        FlowPane buttons = new FlowPane(Theme.SPACING_MD, Theme.SPACING_SM);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        AppButton cancelBtn = AppButton.ghost("Cancel");
        cancelBtn.setOnAction(e -> {
            result.set(false);
            dialogStage.close();
        });

        AppButton confirmBtn = isDanger ? AppButton.danger(confirmText) : AppButton.primary(confirmText);
        confirmBtn.setOnAction(e -> {
            result.set(true);
            dialogStage.close();
        });

        buttons.getChildren().addAll(cancelBtn, confirmBtn);
        content.getChildren().addAll(titleLabel, messageLabel, buttons);

        Scene scene = new Scene(content);
        Theme.applyTheme(scene);

        dialogStage.setScene(scene);
        dialogStage.showAndWait();

        return result.get();
    }
}
