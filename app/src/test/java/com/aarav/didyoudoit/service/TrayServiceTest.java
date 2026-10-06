package com.aarav.didyoudoit.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrayServiceTest {

    @Test
    @DisplayName("WindowsTrayServiceImpl lifecycle methods execute with fault tolerance")
    void testTrayServiceLifecycle() {
        TrayService trayService = new WindowsTrayServiceImpl();
        assertNotNull(trayService);

        // In test/headless runner, isSupported returns false safely
        assertDoesNotThrow(() -> {
            trayService.restoreWindow();
            trayService.minimizeToTray();
            trayService.showTrayMessage("Test", "Message");
            trayService.shutdown();
        });
    }

    @Test
    @DisplayName("TrayService shutdown cleans up resources without errors")
    void testTrayServiceShutdown() {
        TrayService trayService = new WindowsTrayServiceImpl();
        assertDoesNotThrow(trayService::shutdown);
        assertNull(trayService.getNativeTrayIcon());
    }
}
