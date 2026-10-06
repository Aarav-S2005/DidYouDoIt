package com.aarav.didyoudoit.repository;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite implementation of {@link DatabaseManager}.
 * Configures connection pragmas (WAL mode, foreign keys, busy timeout)
 * and schema tables for tasks, history, streaks, and settings.
 * Implements FR-23.
 */
public class SqliteDatabaseManager implements DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(SqliteDatabaseManager.class.getName());
    private static final String DEFAULT_APP_DIR_NAME = "DidYouDoIt";
    private static final String DEFAULT_DB_FILE_NAME = "didyoudoit.db";

    private final String dbPath;
    private final String jdbcUrl;

    /**
     * Constructs a manager using the default Windows %APPDATA% path.
     */
    public SqliteDatabaseManager() {
        this(resolveDefaultDatabasePath());
    }

    /**
     * Constructs a manager pointing to a custom database file path or :memory:.
     *
     * @param dbPath path to the SQLite database file or ":memory:"
     */
    public SqliteDatabaseManager(String dbPath) {
        this.dbPath = dbPath;
        this.jdbcUrl = "jdbc:sqlite:" + dbPath;
        ensureDirectoryExists();
        initializeSchema();
    }

    private static String resolveDefaultDatabasePath() {
        String appData = System.getenv("APPDATA");
        Path dir;
        if (appData != null && !appData.isBlank()) {
            dir = Paths.get(appData, DEFAULT_APP_DIR_NAME);
        } else {
            String userHome = System.getProperty("user.home", ".");
            dir = Paths.get(userHome, "." + DEFAULT_APP_DIR_NAME);
        }
        return dir.resolve(DEFAULT_DB_FILE_NAME).toAbsolutePath().toString();
    }

    private void ensureDirectoryExists() {
        if (":memory:".equals(dbPath)) {
            return;
        }
        try {
            File file = new File(dbPath);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                Files.createDirectories(parent.toPath());
                LOGGER.info("Created database directory: " + parent.getAbsolutePath());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create directory for database: " + dbPath, e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA busy_timeout = 5000;");
            if (!":memory:".equals(dbPath)) {
                stmt.execute("PRAGMA journal_mode = WAL;");
                stmt.execute("PRAGMA synchronous = NORMAL;");
            }
        }
        return conn;
    }

    @Override
    public void initializeSchema() {
        LOGGER.info("Initializing SQLite schema at: " + dbPath);
        String createTasksSql = """
            CREATE TABLE IF NOT EXISTS tasks (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                description TEXT,
                category TEXT NOT NULL,
                priority TEXT NOT NULL,
                due_date_time TEXT,
                recurrence_type TEXT,
                recurrence_time TEXT,
                recurrence_day_of_week TEXT,
                status TEXT NOT NULL,
                completed_at TEXT,
                postponed_until TEXT,
                nag_count INTEGER NOT NULL DEFAULT 0,
                escalation_level INTEGER NOT NULL DEFAULT 1,
                custom_nag_message TEXT,
                is_template INTEGER NOT NULL DEFAULT 0,
                parent_template_id TEXT,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL
            );
        """;

        String createIndexSql = """
            CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
            CREATE INDEX IF NOT EXISTS idx_tasks_due ON tasks(due_date_time);
            CREATE INDEX IF NOT EXISTS idx_tasks_template ON tasks(is_template, parent_template_id);
            CREATE INDEX IF NOT EXISTS idx_tasks_category ON tasks(category);
        """;

        String createTaskHistorySql = """
            CREATE TABLE IF NOT EXISTS task_history (
                id TEXT PRIMARY KEY,
                task_id TEXT NOT NULL,
                task_title TEXT NOT NULL,
                category TEXT NOT NULL,
                priority TEXT NOT NULL,
                parent_template_id TEXT,
                scheduled_for TEXT,
                completed_at TEXT NOT NULL,
                was_overdue INTEGER NOT NULL DEFAULT 0,
                nag_count INTEGER NOT NULL DEFAULT 0,
                recorded_at TEXT NOT NULL
            );
            CREATE INDEX IF NOT EXISTS idx_history_completed ON task_history(completed_at);
            CREATE INDEX IF NOT EXISTS idx_history_template ON task_history(parent_template_id);
        """;

        String createStreaksSql = """
            CREATE TABLE IF NOT EXISTS streaks (
                template_id TEXT PRIMARY KEY,
                task_title TEXT NOT NULL,
                current_streak INTEGER NOT NULL DEFAULT 0,
                best_streak INTEGER NOT NULL DEFAULT 0,
                last_completed_date TEXT
            );
        """;

        String createSettingsSql = """
            CREATE TABLE IF NOT EXISTS app_settings (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                personality_type TEXT NOT NULL,
                quiet_hours_enabled INTEGER NOT NULL DEFAULT 0,
                quiet_hours_start TEXT NOT NULL,
                quiet_hours_end TEXT NOT NULL,
                reminders_paused INTEGER NOT NULL DEFAULT 0,
                reminders_paused_until TEXT,
                auto_start_on_boot INTEGER NOT NULL DEFAULT 0,
                persistent_nagging_enabled INTEGER NOT NULL DEFAULT 1,
                escalation_interval_minutes INTEGER NOT NULL DEFAULT 10,
                default_snooze_minutes INTEGER NOT NULL DEFAULT 15,
                updated_at TEXT NOT NULL
            );
        """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createTasksSql);
            stmt.execute(createIndexSql);
            stmt.execute(createTaskHistorySql);
            stmt.execute(createStreaksSql);
            stmt.execute(createSettingsSql);
            LOGGER.info("SQLite schema initialized successfully.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize SQLite schema", e);
            throw new RuntimeException("Schema initialization failed", e);
        }
    }

    @Override
    public String getDatabasePath() {
        return dbPath;
    }

    @Override
    public void close() {
        // SQLite file connection handles closing per connection.
        LOGGER.info("Closing SqliteDatabaseManager for: " + dbPath);
    }
}
