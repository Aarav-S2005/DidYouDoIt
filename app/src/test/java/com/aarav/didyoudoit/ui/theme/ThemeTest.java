package com.aarav.didyoudoit.ui.theme;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThemeTest {

    @Test
    @DisplayName("Theme constants are non-null and valid")
    void testThemeConstants() {
        assertNotNull(Theme.COLOR_PRIMARY);
        assertNotNull(Theme.COLOR_SECONDARY);
        assertNotNull(Theme.COLOR_BACKGROUND);
        assertNotNull(Theme.COLOR_SURFACE);
        assertNotNull(Theme.COLOR_TEXT_PRIMARY);
        assertNotNull(Theme.COLOR_DANGER);
        assertNotNull(Theme.COLOR_SUCCESS);

        assertTrue(Theme.SPACING_SM > 0);
        assertTrue(Theme.SPACING_MD > Theme.SPACING_SM);
        assertTrue(Theme.RADIUS_MD > 0);
        assertNotNull(Theme.SHADOW_SOFT);
    }

    @Test
    @DisplayName("Theme stylesheet resource exists on classpath")
    void testStylesheetResourceExists() {
        assertNotNull(
                Theme.class.getResource(Theme.STYLESHEET_PATH),
                "Theme stylesheet /styles/theme.css must be available on classpath"
        );
    }

    @Test
    @DisplayName("Bundled font resources exist on classpath")
    void testFontResourcesExist() {
        assertNotNull(
                Theme.class.getResource("/fonts/Delius-Regular.ttf"),
                "Delius font must be available"
        );
        assertNotNull(
                Theme.class.getResource("/fonts/IndieFlower-Regular.ttf"),
                "Indie Flower font must be available"
        );
        assertNotNull(
                Theme.class.getResource("/fonts/Cookie-Regular.ttf"),
                "Cookie font must be available"
        );
    }
}
