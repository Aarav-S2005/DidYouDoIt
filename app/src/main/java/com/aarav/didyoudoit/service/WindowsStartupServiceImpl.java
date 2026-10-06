package com.aarav.didyoudoit.service;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Windows implementation of {@link StartupService} registering the application under
 * HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run via JNA.
 * Implements FR-20, NFR-05, NFR-13, NFR-14.
 */
public class WindowsStartupServiceImpl implements StartupService {

    private static final Logger LOGGER = Logger.getLogger(WindowsStartupServiceImpl.class.getName());
    private static final String REGISTRY_RUN_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String APP_REGISTRY_NAME = "DidYouDoIt";

    private final String customCommand;

    public WindowsStartupServiceImpl() {
        this(null);
    }

    public WindowsStartupServiceImpl(String customCommand) {
        this.customCommand = customCommand;
    }

    @Override
    public boolean isAutoStartEnabled() {
        if (!Platform.isWindows()) {
            return false;
        }

        try {
            return Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, REGISTRY_RUN_KEY, APP_REGISTRY_NAME);
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "Failed to query startup registry: " + t.getMessage());
            return false;
        }
    }

    @Override
    public boolean setAutoStartEnabled(boolean enabled) {
        if (!Platform.isWindows()) {
            LOGGER.warning("Startup registration skipped: operating system is not Windows.");
            return false;
        }

        try {
            if (enabled) {
                String command = resolveLaunchCommand();
                Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, REGISTRY_RUN_KEY, APP_REGISTRY_NAME, command);
                LOGGER.info("Registered DidYouDoIt to Windows startup: " + command);
            } else {
                if (isAutoStartEnabled()) {
                    Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER, REGISTRY_RUN_KEY, APP_REGISTRY_NAME);
                    LOGGER.info("Unregistered DidYouDoIt from Windows startup.");
                }
            }
            return true;
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Failed to update Windows startup registry: " + t.getMessage(), t);
            return false;
        }
    }

    private String resolveLaunchCommand() {
        if (customCommand != null && !customCommand.isBlank()) {
            return customCommand;
        }

        // Check if running as packaged jpackage executable
        String processCommand = ProcessHandle.current().info().command().orElse("");
        if (processCommand.endsWith(".exe") && !processCommand.toLowerCase().contains("java")) {
            return "\"" + processCommand + "\"";
        }

        // Otherwise resolve JAR / Java launcher command
        try {
            File jarFile = new File(WindowsStartupServiceImpl.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (jarFile.isFile() && jarFile.getName().endsWith(".jar")) {
                String javaHome = System.getProperty("java.home");
                String javaw = javaHome + File.separator + "bin" + File.separator + "javaw.exe";
                if (!new File(javaw).exists()) {
                    javaw = "javaw.exe";
                }
                return "\"" + javaw + "\" -jar \"" + jarFile.getAbsolutePath() + "\"";
            }
        } catch (Exception ignored) {
        }

        return "\"" + (processCommand.isBlank() ? "DidYouDoIt.exe" : processCommand) + "\"";
    }
}
