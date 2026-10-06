package com.aarav.didyoudoit.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LinuxStartupServiceTest {

    private Path tempAutostartDir;

    @BeforeEach
    void setUp() throws IOException {
        tempAutostartDir = Files.createTempDirectory("didyoudoit_autostart_test");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(tempAutostartDir)) {
            try (var s = Files.walk(tempAutostartDir)) {
                s.sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                        });
            }
        }
    }

    @Test
    @DisplayName("LinuxStartupService creates and toggles XDG autostart desktop entry")
    void testLinuxAutostartToggle() throws IOException {
        LinuxStartupServiceImpl service = new LinuxStartupServiceImpl(tempAutostartDir, "/usr/bin/didyoudoit");

        assertFalse(service.isAutoStartEnabled(), "Should be disabled initially");

        // Enable autostart
        boolean setTrue = service.setAutoStartEnabled(true);
        assertTrue(setTrue);
        assertTrue(service.isAutoStartEnabled(), "Should be enabled after setting true");

        Path desktopFile = tempAutostartDir.resolve("didyoudoit.desktop");
        assertTrue(Files.exists(desktopFile));

        String content = Files.readString(desktopFile);
        assertTrue(content.contains("[Desktop Entry]"));
        assertTrue(content.contains("Name=DidYouDoIt"));
        assertTrue(content.contains("Exec=/usr/bin/didyoudoit"));
        assertTrue(content.contains("X-GNOME-Autostart-enabled=true"));

        // Disable autostart
        boolean setFalse = service.setAutoStartEnabled(false);
        assertTrue(setFalse);
        assertFalse(service.isAutoStartEnabled(), "Should be disabled after setting false");
        assertFalse(Files.exists(desktopFile));
    }
}
