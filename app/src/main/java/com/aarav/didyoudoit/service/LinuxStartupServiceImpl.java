package com.aarav.didyoudoit.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Linux implementation of {@link StartupService} adhering to the Freedesktop.org
 * XDG Autostart specification by managing ~/.config/autostart/didyoudoit.desktop.
 * Compatible with all major Linux desktop environments (GNOME, KDE Plasma, XFCE, Cinnamon, MATE, etc.).
 * Implements FR-20, NFR-05, NFR-12, NFR-14.
 */
public class LinuxStartupServiceImpl implements StartupService {

    private static final Logger LOGGER = Logger.getLogger(LinuxStartupServiceImpl.class.getName());
    private static final String DESKTOP_FILE_NAME = "didyoudoit.desktop";

    private final Path autostartDir;
    private final String customExecCommand;

    public LinuxStartupServiceImpl() {
        this(null, null);
    }

    public LinuxStartupServiceImpl(Path customAutostartDir, String customExecCommand) {
        if (customAutostartDir != null) {
            this.autostartDir = customAutostartDir;
        } else {
            String configHome = System.getenv("XDG_CONFIG_HOME");
            if (configHome != null && !configHome.isBlank()) {
                this.autostartDir = Paths.get(configHome, "autostart");
            } else {
                this.autostartDir = Paths.get(System.getProperty("user.home", "."), ".config", "autostart");
            }
        }
        this.customExecCommand = customExecCommand;
    }

    private Path getDesktopFilePath() {
        return autostartDir.resolve(DESKTOP_FILE_NAME);
    }

    @Override
    public boolean isAutoStartEnabled() {
        Path file = getDesktopFilePath();
        if (!Files.exists(file)) {
            return false;
        }
        try {
            String content = Files.readString(file);
            return content.contains("X-GNOME-Autostart-enabled=true") || !content.contains("Hidden=true");
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Failed to read Linux autostart desktop entry: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean setAutoStartEnabled(boolean enabled) {
        Path file = getDesktopFilePath();
        try {
            if (enabled) {
                Files.createDirectories(autostartDir);
                String exec = resolveExecCommand();
                String desktopContent = """
                        [Desktop Entry]
                        Type=Application
                        Version=1.0
                        Name=DidYouDoIt
                        GenericName=Personal Accountability & Task Nagging
                        Comment=Desktop accountability assistant that ensures you complete planned tasks
                        Exec=%s
                        Icon=didyoudoit
                        Terminal=false
                        StartupNotify=false
                        Categories=Utility;Office;ProjectManagement;
                        X-GNOME-Autostart-enabled=true
                        """.formatted(exec);
                Files.writeString(file, desktopContent);
                LOGGER.info("Registered DidYouDoIt in Linux autostart: " + file);
            } else {
                if (Files.exists(file)) {
                    Files.delete(file);
                    LOGGER.info("Unregistered DidYouDoIt from Linux autostart.");
                }
            }
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to update Linux autostart entry: " + e.getMessage(), e);
            return false;
        }
    }

    private String resolveExecCommand() {
        if (customExecCommand != null && !customExecCommand.isBlank()) {
            return customExecCommand;
        }

        String processCmd = ProcessHandle.current().info().command().orElse("");
        if (!processCmd.isBlank() && !processCmd.toLowerCase().contains("java")) {
            return processCmd;
        }

        try {
            File jarFile = new File(LinuxStartupServiceImpl.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (jarFile.isFile() && jarFile.getName().endsWith(".jar")) {
                String javaHome = System.getProperty("java.home");
                String javaBin = javaHome + "/bin/java";
                return javaBin + " -jar " + jarFile.getAbsolutePath();
            }
        } catch (Exception ignored) {
        }

        return "didyoudoit";
    }
}
