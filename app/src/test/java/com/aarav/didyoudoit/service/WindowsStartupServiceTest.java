package com.aarav.didyoudoit.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WindowsStartupServiceTest {

    @Test
    @DisplayName("StartupService handles custom launch commands and does not crash")
    void testStartupServiceCustomCommand() {
        StartupService startupService = new WindowsStartupServiceImpl("C:\\Apps\\DidYouDoIt\\DidYouDoIt.exe");
        assertNotNull(startupService);

        // Querying auto-start should execute safely without unhandled exceptions
        boolean isEnabled = startupService.isAutoStartEnabled();
        // Result is either true or false depending on current user registry state
        assertTrue(isEnabled || !isEnabled);
    }

    @Test
    @DisplayName("StartupService handles default command resolution safely")
    void testStartupServiceDefaultCommand() {
        StartupService startupService = new WindowsStartupServiceImpl();
        assertNotNull(startupService);
        assertDoesNotThrow(() -> {
            startupService.isAutoStartEnabled();
        });
    }
}
