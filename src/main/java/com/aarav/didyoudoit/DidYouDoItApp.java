package com.aarav.didyoudoit;

import com.aarav.didyoudoit.ui.theme.FontManager;
import com.aarav.didyoudoit.ui.theme.Theme;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.logging.Logger;

/**
 * Main application entry point for DidYouDoIt.
 * Built without FXML, verifying the central Theme, bundled Google fonts,
 * and warm/playful design foundation.
 */
public class DidYouDoItApp extends Application {

    private static final Logger LOGGER = Logger.getLogger(DidYouDoItApp.class.getName());

    @Override
    public void start(Stage primaryStage) {
        LOGGER.info("Starting DidYouDoIt Application...");

        // Root container
        VBox root = new VBox(Theme.SPACING_LG);
        root.getStyleClass().add("app-container");
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(Theme.SPACING_XL));

        // Header section (Cookie hero font + Indie Flower accent subtitle)
        VBox headerBox = new VBox(Theme.SPACING_XS);
        headerBox.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("DidYouDoIt?");
        titleLabel.setFont(FontManager.getHeroFont(54));
        titleLabel.getStyleClass().add("heading-title");

        Label subtitleLabel = new Label("Your personal accountability and habit companion");
        subtitleLabel.setFont(FontManager.getAccentFont(20));
        subtitleLabel.getStyleClass().add("heading-subtitle");

        headerBox.getChildren().addAll(titleLabel, subtitleLabel);

        // Main Card (Surface) showcasing phase 1 deliverables
        VBox card = new VBox(Theme.SPACING_MD);
        card.getStyleClass().add("card-elevated");
        card.setMaxWidth(680);
        card.setAlignment(Pos.TOP_LEFT);

        Label cardHeader = new Label("Phase 1: Design System & Project Foundation");
        cardHeader.setFont(FontManager.getPrimaryFont(18));
        cardHeader.getStyleClass().add("section-header");

        Label bodyText = new Label(
                "JavaFX 21, SQLite JDBC, and JNA are configured. Google Fonts (Delius, Indie Flower, Cookie) " +
                "are loaded from classpath resources. Pure Java layout active without FXML."
        );
        bodyText.setFont(FontManager.getPrimaryFont(14));
        bodyText.getStyleClass().add("body-text");
        bodyText.setWrapText(true);

        // Badge row demonstrating semantic priority tokens
        Label badgesTitle = new Label("Priority Badges:");
        badgesTitle.setFont(FontManager.getPrimaryFont(13));
        badgesTitle.getStyleClass().add("caption-text");

        HBox badgeRow = new HBox(Theme.SPACING_SM);
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        Label urgentBadge = new Label("Urgent");
        urgentBadge.getStyleClass().addAll("badge", "badge-urgent");

        Label highBadge = new Label("High");
        highBadge.getStyleClass().addAll("badge", "badge-high");

        Label mediumBadge = new Label("Medium");
        mediumBadge.getStyleClass().addAll("badge", "badge-medium");

        Label lowBadge = new Label("Low");
        lowBadge.getStyleClass().addAll("badge", "badge-low");

        Label successBadge = new Label("Completed");
        successBadge.getStyleClass().addAll("badge", "badge-success");

        badgeRow.getChildren().addAll(urgentBadge, highBadge, mediumBadge, lowBadge, successBadge);

        // Color swatches row
        Label paletteTitle = new Label("Color Palette Tokens:");
        paletteTitle.setFont(FontManager.getPrimaryFont(13));
        paletteTitle.getStyleClass().add("caption-text");

        HBox swatchRow = new HBox(Theme.SPACING_MD);
        swatchRow.setAlignment(Pos.CENTER_LEFT);
        swatchRow.getChildren().addAll(
                createSwatch(Theme.COLOR_PRIMARY, "Terracotta"),
                createSwatch(Theme.COLOR_SECONDARY, "Sage Mint"),
                createSwatch(Theme.COLOR_TEXT_PRIMARY, "Warm Slate"),
                createSwatch(Theme.COLOR_BACKGROUND, "Linen Cream"),
                createSwatch(Theme.COLOR_DANGER, "Overdue Red")
        );

        // Buttons row
        HBox buttonRow = new HBox(Theme.SPACING_MD);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        Button primaryBtn = new Button("Primary Action");
        primaryBtn.getStyleClass().addAll("button", "btn-primary");

        Button secondaryBtn = new Button("Secondary Action");
        secondaryBtn.getStyleClass().addAll("button", "btn-secondary");

        Button ghostBtn = new Button("Ghost Button");
        ghostBtn.getStyleClass().addAll("button", "btn-ghost");

        Label clickFeedback = new Label("Ready");
        clickFeedback.setFont(FontManager.getAccentFont(15));
        clickFeedback.getStyleClass().add("font-accent");

        primaryBtn.setOnAction(e -> clickFeedback.setText("Primary clicked!"));
        secondaryBtn.setOnAction(e -> clickFeedback.setText("Secondary clicked!"));
        ghostBtn.setOnAction(e -> clickFeedback.setText("Ghost clicked!"));

        buttonRow.getChildren().addAll(primaryBtn, secondaryBtn, ghostBtn, clickFeedback);

        card.getChildren().addAll(
                cardHeader,
                bodyText,
                badgesTitle,
                badgeRow,
                paletteTitle,
                swatchRow,
                buttonRow
        );

        // Footer note
        Label footerNote = new Label("Ready for Phase 2: Domain Models & Persistence Layer (SQLite + JDBC)");
        footerNote.setFont(FontManager.getAccentFont(16));
        footerNote.getStyleClass().add("font-accent");

        root.getChildren().addAll(headerBox, card, footerNote);
        VBox.setVgrow(card, Priority.NEVER);

        Scene scene = new Scene(root, 760, 620);
        Theme.applyTheme(scene);

        primaryStage.setTitle("DidYouDoIt? - Personal Accountability");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(640);
        primaryStage.setMinHeight(520);
        primaryStage.show();

        LOGGER.info("DidYouDoIt Window displayed successfully.");
    }

    private HBox createSwatch(javafx.scene.paint.Color color, String name) {
        Circle circle = new Circle(8, color);
        circle.setStroke(Theme.COLOR_BORDER);
        circle.setStrokeWidth(1);

        Label label = new Label(name);
        label.setFont(FontManager.getPrimaryFont(12));
        label.getStyleClass().add("caption-text");

        HBox box = new HBox(Theme.SPACING_XS, circle, label);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
