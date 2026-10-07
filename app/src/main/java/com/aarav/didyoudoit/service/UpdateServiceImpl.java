package com.aarav.didyoudoit.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aarav.didyoudoit.util.AppVersion;

/**
 * Implementation of UpdateService querying GitHub Releases API asynchronously.
 * Fully non-blocking with zero third-party JSON/HTTP dependencies (Java 21 native HttpClient).
 */
public class UpdateServiceImpl implements UpdateService {

    private static final Logger LOGGER = Logger.getLogger(UpdateServiceImpl.class.getName());
    private static final String DEFAULT_REPO = "Aarav-S2005/DidYouDoIt";

    private final String repository;
    private final HttpClient httpClient;

    public UpdateServiceImpl() {
        this(DEFAULT_REPO);
    }

    public UpdateServiceImpl(String repository) {
        this.repository = repository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    @Override
    public String getCurrentVersion() {
        return AppVersion.get();
    }

    @Override
    public CompletableFuture<Path> downloadAssetAsync(String downloadUrl, Consumer<Double> progressConsumer) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(downloadUrl))
                        .header("User-Agent", "DidYouDoIt-Desktop/" + getCurrentVersion())
                        .timeout(Duration.ofMinutes(5))
                        .GET()
                        .build();

                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() >= 400) {
                    throw new IOException("HTTP error: " + response.statusCode());
                }

                long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
                String extension = downloadUrl.endsWith(".msi") ? ".msi"
                        : (downloadUrl.endsWith(".deb") ? ".deb"
                        : (downloadUrl.endsWith(".zip") ? ".zip"
                        : (downloadUrl.endsWith(".tar.gz") ? ".tar.gz" : ".tmp")));

                Path tempFile = Files.createTempFile("DidYouDoIt_Update_", extension);
                tempFile.toFile().deleteOnExit();

                try (InputStream in = response.body();
                     OutputStream out = Files.newOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    long totalRead = 0;
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalRead += bytesRead;
                        if (contentLength > 0 && progressConsumer != null) {
                            double progress = (double) totalRead / contentLength;
                            progressConsumer.accept(progress);
                        }
                    }
                }

                if (progressConsumer != null) {
                    progressConsumer.accept(1.0);
                }
                return tempFile;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to download update asset: " + e.getMessage(), e);
                throw new RuntimeException("Download failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<UpdateInfo> checkForUpdatesAsync() {
        return CompletableFuture.supplyAsync(() -> {
            String currentVersion = getCurrentVersion();
            try {
                String apiUrl = "https://api.github.com/repos/" + repository + "/releases/latest";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Accept", "application/vnd.github.v3+json")
                        .header("User-Agent", "DidYouDoIt-Desktop/" + currentVersion)
                        .timeout(Duration.ofSeconds(8))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String json = response.body();
                    String tagName = extractJsonField(json, "tag_name");
                    String htmlUrl = extractJsonField(json, "html_url");
                    String body = extractJsonField(json, "body");

                    if (tagName != null && !tagName.isBlank()) {
                        String cleanLatest = tagName.replaceFirst("^[vV]", "");
                        String cleanCurrent = currentVersion.replaceFirst("^[vV]", "");

                        boolean newer = isVersionNewer(cleanCurrent, cleanLatest);

                        // Extract all release asset types
                        String msiUrl = extractRegex(json, "\"browser_download_url\":\\s*\"([^\"]+\\.msi)\"");
                        String zipUrl = extractRegex(json, "\"browser_download_url\":\\s*\"([^\"]+\\.zip)\"");
                        String debUrl = extractRegex(json, "\"browser_download_url\":\\s*\"([^\"]+\\.deb)\"");
                        String tarGzUrl = extractRegex(json, "\"browser_download_url\":\\s*\"([^\"]+\\.tar\\.gz)\"");

                        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
                        String downloadUrl;
                        if (isWindows) {
                            downloadUrl = msiUrl != null ? msiUrl : (zipUrl != null ? zipUrl : htmlUrl);
                        } else {
                            downloadUrl = debUrl != null ? debUrl : (tarGzUrl != null ? tarGzUrl : htmlUrl);
                        }

                        return new UpdateInfo(
                                newer,
                                currentVersion,
                                tagName,
                                htmlUrl,
                                downloadUrl,
                                msiUrl,
                                zipUrl,
                                debUrl,
                                tarGzUrl,
                                body != null ? body : ""
                        );
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Update check skipped or offline: " + e.getMessage());
            }

            // Fallback when offline or rate-limited
            return new UpdateInfo(
                    false,
                    currentVersion,
                    "v" + currentVersion,
                    "https://github.com/" + repository + "/releases",
                    "https://github.com/" + repository + "/releases",
                    null, null, null, null,
                    "Currently up to date."
            );
        });
    }

    static boolean isVersionNewer(String current, String latest) {
        String[] currParts = current.split("\\.");
        String[] lateParts = latest.split("\\.");

        int length = Math.max(currParts.length, lateParts.length);
        for (int i = 0; i < length; i++) {
            int c = i < currParts.length ? parseSafeInt(currParts[i]) : 0;
            int l = i < lateParts.length ? parseSafeInt(lateParts[i]) : 0;
            if (l > c) return true;
            if (l < c) return false;
        }
        return false;
    }

    private static int parseSafeInt(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private static String extractJsonField(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String extractRegex(String text, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }
}
