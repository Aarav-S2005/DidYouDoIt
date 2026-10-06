package com.aarav.didyoudoit.ui.components;

import com.aarav.didyoudoit.ui.theme.FontManager;
import com.aarav.didyoudoit.ui.theme.Theme;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Reusable text input field with title label, placeholder, and inline error validation feedback.
 */
public class InputField extends VBox {

    private final Label titleLabel;
    private final TextField textField;
    private final Label errorLabel;

    public InputField(String title, String placeholder) {
        super(Theme.SPACING_XS);

        this.titleLabel = new Label(Objects.requireNonNullElse(title, ""));
        this.titleLabel.setFont(FontManager.getPrimaryFont(13));
        this.titleLabel.getStyleClass().add("caption-text");

        this.textField = new TextField();
        this.textField.setPromptText(Objects.requireNonNullElse(placeholder, ""));
        this.textField.setFont(FontManager.getPrimaryFont(14));
        this.textField.getStyleClass().add("input-text");

        this.errorLabel = new Label();
        this.errorLabel.setFont(FontManager.getPrimaryFont(11));
        this.errorLabel.getStyleClass().add("error-text");
        this.errorLabel.setVisible(false);
        this.errorLabel.setManaged(false);

        getChildren().addAll(titleLabel, textField, errorLabel);

        // Clear error on user edit
        this.textField.textProperty().addListener((obs, oldVal, newVal) -> clearError());
    }

    public String getText() {
        return textField.getText();
    }

    public void setText(String text) {
        textField.setText(text);
    }

    public TextField getTextField() {
        return textField;
    }

    public void setError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            clearError();
        } else {
            errorLabel.setText(errorMessage);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            textField.getStyleClass().add("input-error");
        }
    }

    public void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        textField.getStyleClass().remove("input-error");
    }
}
