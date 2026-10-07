package com.aarav.didyoudoit.util;

import java.io.InputStream;
import java.util.Properties;

/**
 * Utility for resolving the application's runtime version dynamically.
 * Reads from packaged Maven pom.properties or Package implementation version,
 * ensuring the UI always accurately displays the exact installed version.
 */
public final class AppVersion {

    public static final String CURRENT_VERSION = "1.1.2";
    private static String resolvedVersion;

    private AppVersion() {}

    /**
     * Returns the dynamic runtime version of the application.
     */
    public static synchronized String get() {
        if (resolvedVersion != null) {
            return resolvedVersion;
        }

        // 1. Try reading Maven pom.properties generated at packaging time
        try (InputStream is = AppVersion.class.getResourceAsStream("/META-INF/maven/com.aarav/DidYouDoIt/pom.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String ver = props.getProperty("version");
                if (ver != null && !ver.isBlank()) {
                    resolvedVersion = ver.trim();
                    return resolvedVersion;
                }
            }
        } catch (Exception ignored) {
        }

        // 2. Try Package implementation version
        Package pkg = AppVersion.class.getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null && !pkg.getImplementationVersion().isBlank()) {
            resolvedVersion = pkg.getImplementationVersion().trim();
            return resolvedVersion;
        }

        // 3. Fallback to defined constant
        resolvedVersion = CURRENT_VERSION;
        return resolvedVersion;
    }
}
