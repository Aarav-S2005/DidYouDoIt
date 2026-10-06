package com.aarav.didyoudoit.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AppIconGeneratorTest {

    @Test
    @DisplayName("Generates valid high-DPI icon images")
    void testGenerateIconImage() {
        BufferedImage img = AppIconGenerator.generateIconImage(64);
        assertNotNull(img);
        assertEquals(64, img.getWidth());
        assertEquals(64, img.getHeight());
    }

    @Test
    @DisplayName("Generates PNG assets in target resource directory")
    void testGenerateAndSave() throws IOException {
        Path targetDir = Path.of("src/main/resources/icons");
        AppIconGenerator.generateAndSave(targetDir);

        assertTrue(Files.exists(targetDir.resolve("app-icon.png")));
        assertTrue(Files.exists(targetDir.resolve("app-icon-32.png")));
        assertTrue(Files.exists(targetDir.resolve("app-icon-16.png")));
    }
}
