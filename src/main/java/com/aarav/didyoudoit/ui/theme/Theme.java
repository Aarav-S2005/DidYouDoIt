package com.aarav.didyoudoit.ui.theme;

import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Central design tokens and styling helper for DidYouDoIt.
 * Defines colors, spacing, corner radii, shadow effects, and stylesheet loading.
 */
public final class Theme {

    private static final Logger LOGGER = Logger.getLogger(Theme.class.getName());

    public static final String STYLESHEET_PATH = "/styles/theme.css";

    // --- Color Palette ---
    public static final Color COLOR_BACKGROUND = Color.web("#FAF7F2");      // Warm Linen / Cream
    public static final Color COLOR_SURFACE = Color.web("#FFFFFF");         // Pure White Card
    public static final Color COLOR_SURFACE_ALT = Color.web("#F3EFEA");     // Slightly deeper cream
    public static final Color COLOR_BORDER = Color.web("#E5DFD7");          // Warm subtle border
    public static final Color COLOR_BORDER_FOCUS = Color.web("#E27D60");    // Terracotta active border

    public static final Color COLOR_PRIMARY = Color.web("#E27D60");         // Warm Terracotta / Coral
    public static final Color COLOR_PRIMARY_HOVER = Color.web("#D46B4E");   // Deeper terracotta
    public static final Color COLOR_SECONDARY = Color.web("#85B79D");       // Sage / Mint
    public static final Color COLOR_SECONDARY_HOVER = Color.web("#74A68C");

    public static final Color COLOR_TEXT_PRIMARY = Color.web("#2B2D42");    // Deep Charcoal / Slate
    public static final Color COLOR_TEXT_SECONDARY = Color.web("#6C757D");  // Muted Warm Grey
    public static final Color COLOR_TEXT_MUTED = Color.web("#9E9E9E");
    public static final Color COLOR_TEXT_ON_PRIMARY = Color.web("#FFFFFF");

    // Semantic Status Colors
    public static final Color COLOR_SUCCESS = Color.web("#2A9D8F");         // Green / Complete
    public static final Color COLOR_WARNING = Color.web("#E9C46A");         // Golden Amber
    public static final Color COLOR_DANGER = Color.web("#E76F51");          // Red / Overdue / Nagging
    public static final Color COLOR_INFO = Color.web("#41B3A3");            // Teal / Info

    // --- Spacing Scale (pixels) ---
    public static final double SPACING_XS = 4.0;
    public static final double SPACING_SM = 8.0;
    public static final double SPACING_MD = 16.0;
    public static final double SPACING_LG = 24.0;
    public static final double SPACING_XL = 32.0;

    // --- Corner Radii (pixels) ---
    public static final double RADIUS_SM = 6.0;
    public static final double RADIUS_MD = 12.0;
    public static final double RADIUS_LG = 18.0;
    public static final double RADIUS_PILL = 999.0;

    // --- Shadows ---
    public static final DropShadow SHADOW_SOFT = new DropShadow(
            12.0, 0.0, 4.0, Color.rgb(43, 45, 66, 0.08)
    );

    public static final DropShadow SHADOW_HOVER = new DropShadow(
            18.0, 0.0, 6.0, Color.rgb(43, 45, 66, 0.14)
    );

    public static final DropShadow SHADOW_FLOATING = new DropShadow(
            24.0, 0.0, 10.0, Color.rgb(43, 45, 66, 0.18)
    );

    private Theme() {
        // Utility class
    }

    /**
     * Applies the central theme stylesheet and font initialization to a Scene.
     */
    public static void applyTheme(Scene scene) {
        Objects.requireNonNull(scene, "scene cannot be null");
        FontManager.initialize();

        URL cssUrl = Theme.class.getResource(STYLESHEET_PATH);
        if (cssUrl != null) {
            String cssPath = cssUrl.toExternalForm();
            if (!scene.getStylesheets().contains(cssPath)) {
                scene.getStylesheets().add(cssPath);
            }
        } else {
            LOGGER.warning("Could not find theme stylesheet at: " + STYLESHEET_PATH);
        }
    }
}
