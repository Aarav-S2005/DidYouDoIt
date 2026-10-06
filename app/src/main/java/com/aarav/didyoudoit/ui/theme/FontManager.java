package com.aarav.didyoudoit.ui.theme;

import javafx.scene.text.Font;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the loading and retrieval of custom bundled fonts for DidYouDoIt.
 * Fonts loaded:
 * - Delius (Primary UI and body)
 * - Indie Flower (Accents, subtitles, tooltips, empty states)
 * - Cookie (Hero headings and logo only)
 */
public final class FontManager {

    private static final Logger LOGGER = Logger.getLogger(FontManager.class.getName());

    public static final String FONT_FAMILY_PRIMARY = "Delius";
    public static final String FONT_FAMILY_ACCENT = "Indie Flower";
    public static final String FONT_FAMILY_HERO = "Cookie";

    private static boolean initialized = false;

    private FontManager() {
        // Utility class
    }

    /**
     * Loads the bundled TrueType fonts from classpath into JavaFX font cache.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        loadFontResource("/fonts/Delius-Regular.ttf", FONT_FAMILY_PRIMARY);
        loadFontResource("/fonts/IndieFlower-Regular.ttf", FONT_FAMILY_ACCENT);
        loadFontResource("/fonts/Cookie-Regular.ttf", FONT_FAMILY_HERO);

        initialized = true;
    }

    private static void loadFontResource(String resourcePath, String expectedFamily) {
        try (InputStream stream = FontManager.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                LOGGER.log(Level.WARNING, "Font resource not found: {0}", resourcePath);
                return;
            }
            Font loaded = Font.loadFont(stream, 14);
            if (loaded != null) {
                LOGGER.log(Level.INFO, "Loaded font: {0} (family: {1})",
                        new Object[]{loaded.getName(), loaded.getFamily()});
            } else {
                LOGGER.log(Level.WARNING, "Failed to load font from: {0}", resourcePath);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception while loading font from: " + resourcePath, e);
        }
    }

    public static Font getHeroFont(double size) {
        initialize();
        Font font = Font.font(FONT_FAMILY_HERO, size);
        return (font != null && font.getFamily().equalsIgnoreCase(FONT_FAMILY_HERO))
                ? font
                : Font.font("System", size);
    }

    public static Font getAccentFont(double size) {
        initialize();
        Font font = Font.font(FONT_FAMILY_ACCENT, size);
        return (font != null && font.getFamily().equalsIgnoreCase(FONT_FAMILY_ACCENT))
                ? font
                : Font.font("System", size);
    }

    public static Font getPrimaryFont(double size) {
        initialize();
        Font font = Font.font(FONT_FAMILY_PRIMARY, size);
        return (font != null && font.getFamily().equalsIgnoreCase(FONT_FAMILY_PRIMARY))
                ? font
                : Font.font("System", size);
    }
}
