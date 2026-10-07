package com.aarav.didyoudoit.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of UpdateService querying GitHub Releases API asynchronously.
 * Fully non-blocking with zero third-party JSON/HTTP dependencies (Java 21 native HttpClient).
 */
public class UpdateServiceImpl implements UpdateService {

    private static final Logger LOGGER = Logger.getLogger(UpdateServiceImpl.class.getName());
    private static final String DEFAULT_REPO = "Aarav-S2005/DidYouDoIt";
    private static final String APP_VERSION = "1.1.0";

    private final String repository;
    private final HttpClient httpClient;

    public UpdateServiceImpl() {
        this(DEFAULT_REPO);
    }

    public UpdateServiceImpl(String repository) {
        this.repository = repository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String getCurrentVersion() {
        return APP_VERSION;
    }

    @Override
    public CompletableFuture<UpdateInfo> checkForUpdatesAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String apiUrl = "https://api.github.com/repos/" + repository + "/releases/latest";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Accept", "application/vnd.github.v3+json")
                        .header("User-Agent", "DidYouDoIt-Desktop/" + APP_VERSION)
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
                        String cleanCurrent = APP_VERSION.replaceFirst("^[vV]", "");

                        boolean newer = isVersionNewer(cleanCurrent, cleanLatest);

                        // Find matching asset download URL based on OS
                        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
                        String assetRegex = isWindows ? "\"browser_download_url\":\\s*\"([^\"]+\\.zip)\"" : "\"browser_download_url\":\\s*\"([^\"]+\\.deb)\"";
                        Pattern p = Pattern.compile(assetRegex);
                        Matcher m = p.matcher(json);
                        String downloadUrl = m.find() ? m.group(1) : htmlUrl;

                        return new UpdateInfo(newer, APP_VERSION, tagName, htmlUrl, downloadUrl, body != null ? body : "");
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Update check skipped or offline: " + e.getMessage());
            }

            // Fallback when offline or rate-limited
            return new UpdateInfo(
                    false,
                    APP_VERSION,
                    "v" + APP_VERSION,
                    "https://github.com/" + repository + "/releases",
                    "https://github.com/" + repository + "/releases",
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
}
