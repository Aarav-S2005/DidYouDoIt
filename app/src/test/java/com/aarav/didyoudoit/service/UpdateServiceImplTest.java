package com.aarav.didyoudoit.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UpdateServiceImpl Unit Tests")
class UpdateServiceImplTest {

    @Test
    @DisplayName("isVersionNewer correctly detects newer semantic versions")
    void testIsVersionNewer() {
        assertTrue(UpdateServiceImpl.isVersionNewer("1.0.1", "1.0.2"));
        assertTrue(UpdateServiceImpl.isVersionNewer("1.0.1", "1.1.0"));
        assertTrue(UpdateServiceImpl.isVersionNewer("1.0.1", "2.0.0"));
        assertTrue(UpdateServiceImpl.isVersionNewer("1.0.1", "1.0.1.1"));

        assertFalse(UpdateServiceImpl.isVersionNewer("1.0.2", "1.0.1"));
        assertFalse(UpdateServiceImpl.isVersionNewer("1.1.0", "1.0.9"));
        assertFalse(UpdateServiceImpl.isVersionNewer("1.0.1", "1.0.1"));
    }

    @Test
    @DisplayName("UpdateService correctly reports dynamic application version")
    void testCurrentVersion() {
        UpdateService service = new UpdateServiceImpl();
        assertNotNull(service.getCurrentVersion());
        assertFalse(service.getCurrentVersion().isBlank());
    }
}
