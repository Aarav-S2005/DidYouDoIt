package com.aarav.didyoudoit.service;

import java.util.concurrent.CompletableFuture;

/**
 * Service providing in-app update checks against GitHub Releases.
 * Implements NFR-11, NFR-12, NFR-17.
 */
public interface UpdateService {

    record UpdateInfo(
            boolean updateAvailable,
            String currentVersion,
            String latestVersion,
            String releaseUrl,
            String downloadUrl,
            String releaseNotes
    ) {}

    /**
     * Current running application version.
     */
    String getCurrentVersion();

    /**
     * Checks for updates asynchronously without blocking the UI thread.
     */
    CompletableFuture<UpdateInfo> checkForUpdatesAsync();
}
