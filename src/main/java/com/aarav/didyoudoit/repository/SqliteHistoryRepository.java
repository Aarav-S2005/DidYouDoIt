package com.aarav.didyoudoit.repository;

import com.aarav.didyoudoit.model.Priority;
import com.aarav.didyoudoit.model.StreakInfo;
import com.aarav.didyoudoit.model.TaskCategory;
import com.aarav.didyoudoit.model.TaskHistory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite implementation of {@link HistoryRepository}.
 * Implements FR-13, FR-14, FR-22, FR-23.
 */
public class SqliteHistoryRepository implements HistoryRepository {

    private static final Logger LOGGER = Logger.getLogger(SqliteHistoryRepository.class.getName());

    private final DatabaseManager databaseManager;

    public SqliteHistoryRepository(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager cannot be null");
    }

    @Override
    public void logCompletion(TaskHistory history) {
        Objects.requireNonNull(history, "history cannot be null");
        String sql = """
            INSERT INTO task_history (
                id, task_id, task_title, category, priority, parent_template_id,
                scheduled_for, completed_at, was_overdue, nag_count, recorded_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, history.getId());
            ps.setString(2, history.getTaskId());
            ps.setString(3, history.getTaskTitle());
            ps.setString(4, history.getCategory().name());
            ps.setString(5, history.getPriority().name());
            ps.setString(6, history.getParentTemplateId());
            ps.setString(7, history.getScheduledFor() != null ? history.getScheduledFor().toString() : null);
            ps.setString(8, history.getCompletedAt().toString());
            ps.setInt(9, history.isWasOverdue() ? 1 : 0);
            ps.setInt(10, history.getNagCount());
            ps.setString(11, history.getRecordedAt().toString());

            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to log task completion history", e);
            throw new RuntimeException("Could not persist task history", e);
        }
    }

    @Override
    public List<TaskHistory> findRecentHistory(int limit) {
        String sql = "SELECT * FROM task_history ORDER BY completed_at DESC LIMIT ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                List<TaskHistory> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRowToHistory(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve recent history", e);
            return List.of();
        }
    }

    @Override
    public List<TaskHistory> findHistoryForDateRange(LocalDate start, LocalDate end) {
        Objects.requireNonNull(start, "start date cannot be null");
        Objects.requireNonNull(end, "end date cannot be null");

        String startStr = start.atStartOfDay().toString();
        String endStr = end.plusDays(1).atStartOfDay().toString();

        String sql = """
            SELECT * FROM task_history
            WHERE completed_at >= ? AND completed_at < ?
            ORDER BY completed_at DESC;
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, startStr);
            ps.setString(2, endStr);
            try (ResultSet rs = ps.executeQuery()) {
                List<TaskHistory> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRowToHistory(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to query history range", e);
            return List.of();
        }
    }

    @Override
    public Optional<StreakInfo> findStreak(String templateId) {
        if (templateId == null || templateId.isBlank()) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM streaks WHERE template_id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, templateId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToStreak(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to query streak for template: " + templateId, e);
        }
        return Optional.empty();
    }

    @Override
    public void saveStreak(StreakInfo streakInfo) {
        Objects.requireNonNull(streakInfo, "streakInfo cannot be null");
        String sql = """
            INSERT INTO streaks (template_id, task_title, current_streak, best_streak, last_completed_date)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(template_id) DO UPDATE SET
                task_title = excluded.task_title,
                current_streak = excluded.current_streak,
                best_streak = excluded.best_streak,
                last_completed_date = excluded.last_completed_date;
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, streakInfo.getTemplateId());
            ps.setString(2, streakInfo.getTaskTitle());
            ps.setInt(3, streakInfo.getCurrentStreak());
            ps.setInt(4, streakInfo.getBestStreak());
            ps.setString(5, streakInfo.getLastCompletedDate() != null ? streakInfo.getLastCompletedDate().toString() : null);

            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to save streak for template: " + streakInfo.getTemplateId(), e);
            throw new RuntimeException("Could not persist streak", e);
        }
    }

    @Override
    public List<StreakInfo> findAllStreaks() {
        String sql = "SELECT * FROM streaks ORDER BY current_streak DESC;";
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<StreakInfo> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRowToStreak(rs));
            }
            return list;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve streaks", e);
            return List.of();
        }
    }

    @Override
    public long countCompletedTasks() {
        return executeCountQuery("SELECT COUNT(1) FROM task_history;");
    }

    @Override
    public long countOverdueCompletions() {
        return executeCountQuery("SELECT COUNT(1) FROM task_history WHERE was_overdue = 1;");
    }

    private long executeCountQuery(String sql) {
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Count query failed: " + sql, e);
        }
        return 0;
    }

    private TaskHistory mapRowToHistory(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String taskId = rs.getString("task_id");
        String title = rs.getString("task_title");
        TaskCategory cat = TaskCategory.fromString(rs.getString("category"));
        Priority priority = Priority.valueOf(rs.getString("priority"));
        String parentId = rs.getString("parent_template_id");

        String schedStr = rs.getString("scheduled_for");
        LocalDateTime scheduledFor = schedStr != null ? LocalDateTime.parse(schedStr) : null;

        LocalDateTime completedAt = LocalDateTime.parse(rs.getString("completed_at"));
        boolean wasOverdue = rs.getInt("was_overdue") == 1;
        int nagCount = rs.getInt("nag_count");
        LocalDateTime recordedAt = LocalDateTime.parse(rs.getString("recorded_at"));

        return new TaskHistory(id, taskId, title, cat, priority, parentId, scheduledFor,
                completedAt, wasOverdue, nagCount, recordedAt);
    }

    private StreakInfo mapRowToStreak(ResultSet rs) throws SQLException {
        String templateId = rs.getString("template_id");
        String title = rs.getString("task_title");
        int currentStreak = rs.getInt("current_streak");
        int bestStreak = rs.getInt("best_streak");
        String dateStr = rs.getString("last_completed_date");
        LocalDate lastDate = dateStr != null ? LocalDate.parse(dateStr) : null;

        return new StreakInfo(templateId, title, currentStreak, bestStreak, lastDate);
    }
}
