package com.aarav.didyoudoit.service;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

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
            String msiUrl,
            String zipUrl,
            String debUrl,
            String tarGzUrl,
            String releaseNotes
    ) {
        public String getPrimaryDownloadUrl(boolean isWindows) {
            if (isWindows) {
                return (msiUrl != null && !msiUrl.isBlank()) ? msiUrl : (zipUrl != null ? zipUrl : releaseUrl);
            } else {
                return (debUrl != null && !debUrl.isBlank()) ? debUrl : (tarGzUrl != null ? tarGzUrl : releaseUrl);
            }
        }
    }

    /**
     * Current running application version.
     */
    String getCurrentVersion();

    /**
     * Checks for updates asynchronously without blocking the UI thread.
     */
    CompletableFuture<UpdateInfo> checkForUpdatesAsync();

    /**
     * Downloads an update asset asynchronously to a temporary file.
     */
    default CompletableFuture<Path> downloadAssetAsync(String downloadUrl, Consumer<Double> progressConsumer) {
        return CompletableFuture.completedFuture(null);
    }
}
