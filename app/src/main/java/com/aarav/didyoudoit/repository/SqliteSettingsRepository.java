package com.aarav.didyoudoit.repository;

import com.aarav.didyoudoit.model.AppSettings;
import com.aarav.didyoudoit.model.PersonalityType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite implementation of {@link SettingsRepository}.
 * Implements FR-15, FR-17, FR-20, FR-23, FR-25.
 */
public class SqliteSettingsRepository implements SettingsRepository {

    private static final Logger LOGGER = Logger.getLogger(SqliteSettingsRepository.class.getName());

    private final DatabaseManager databaseManager;

    public SqliteSettingsRepository(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager cannot be null");
    }

    @Override
    public AppSettings getSettings() {
        String sql = "SELECT * FROM app_settings WHERE id = 1;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                AppSettings settings = new AppSettings();
                settings.setPersonalityType(PersonalityType.fromString(rs.getString("personality_type")));
                settings.setQuietHoursEnabled(rs.getInt("quiet_hours_enabled") == 1);
                settings.setQuietHoursStart(LocalTime.parse(rs.getString("quiet_hours_start")));
                settings.setQuietHoursEnd(LocalTime.parse(rs.getString("quiet_hours_end")));
                settings.setRemindersPaused(rs.getInt("reminders_paused") == 1);

                String pauseUntilStr = rs.getString("reminders_paused_until");
                if (pauseUntilStr != null) {
                    settings.setRemindersPausedUntil(LocalDateTime.parse(pauseUntilStr));
                }

                settings.setAutoStartOnBoot(rs.getInt("auto_start_on_boot") == 1);
                settings.setPersistentNaggingEnabled(rs.getInt("persistent_nagging_enabled") == 1);
                settings.setEscalationIntervalMinutes(rs.getInt("escalation_interval_minutes"));
                settings.setDefaultSnoozeMinutes(rs.getInt("default_snooze_minutes"));
                settings.setUpdatedAt(LocalDateTime.parse(rs.getString("updated_at")));
                return settings;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load app settings from database", e);
        }

        // Return and persist defaults
        AppSettings defaults = AppSettings.createDefault();
        saveSettings(defaults);
        return defaults;
    }

    @Override
    public void saveSettings(AppSettings settings) {
        Objects.requireNonNull(settings, "settings cannot be null");
        String sql = """
            INSERT INTO app_settings (
                id, personality_type, quiet_hours_enabled, quiet_hours_start, quiet_hours_end,
                reminders_paused, reminders_paused_until, auto_start_on_boot,
                persistent_nagging_enabled, escalation_interval_minutes, default_snooze_minutes, updated_at
            ) VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                personality_type = excluded.personality_type,
                quiet_hours_enabled = excluded.quiet_hours_enabled,
                quiet_hours_start = excluded.quiet_hours_start,
                quiet_hours_end = excluded.quiet_hours_end,
                reminders_paused = excluded.reminders_paused,
                reminders_paused_until = excluded.reminders_paused_until,
                auto_start_on_boot = excluded.auto_start_on_boot,
                persistent_nagging_enabled = excluded.persistent_nagging_enabled,
                escalation_interval_minutes = excluded.escalation_interval_minutes,
                default_snooze_minutes = excluded.default_snooze_minutes,
                updated_at = excluded.updated_at;
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, settings.getPersonalityType().name());
            ps.setInt(2, settings.isQuietHoursEnabled() ? 1 : 0);
            ps.setString(3, settings.getQuietHoursStart().toString());
            ps.setString(4, settings.getQuietHoursEnd().toString());
            ps.setInt(5, settings.isRemindersPaused() ? 1 : 0);
            ps.setString(6, settings.getRemindersPausedUntil() != null ? settings.getRemindersPausedUntil().toString() : null);
            ps.setInt(7, settings.isAutoStartOnBoot() ? 1 : 0);
            ps.setInt(8, settings.isPersistentNaggingEnabled() ? 1 : 0);
            ps.setInt(9, settings.getEscalationIntervalMinutes());
            ps.setInt(10, settings.getDefaultSnoozeMinutes());
            ps.setString(11, LocalDateTime.now().toString());

            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to save app settings", e);
            throw new RuntimeException("Could not persist settings", e);
        }
    }
}
