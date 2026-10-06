package com.aarav.didyoudoit.repository;

import com.aarav.didyoudoit.model.*;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite JDBC implementation of {@link TaskRepository}.
 * Implements FR-01, FR-02, FR-03, FR-21, FR-23.
 */
public class SqliteTaskRepository implements TaskRepository {

    private static final Logger LOGGER = Logger.getLogger(SqliteTaskRepository.class.getName());

    private final DatabaseManager databaseManager;

    public SqliteTaskRepository(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "databaseManager cannot be null");
    }

    @Override
    public Task save(Task task) {
        Objects.requireNonNull(task, "task cannot be null");
        String sql = """
            INSERT INTO tasks (
                id, title, description, category, priority, due_date_time,
                recurrence_type, recurrence_time, recurrence_day_of_week,
                status, completed_at, postponed_until, nag_count, escalation_level,
                custom_nag_message, is_template, parent_template_id, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                title = excluded.title,
                description = excluded.description,
                category = excluded.category,
                priority = excluded.priority,
                due_date_time = excluded.due_date_time,
                recurrence_type = excluded.recurrence_type,
                recurrence_time = excluded.recurrence_time,
                recurrence_day_of_week = excluded.recurrence_day_of_week,
                status = excluded.status,
                completed_at = excluded.completed_at,
                postponed_until = excluded.postponed_until,
                nag_count = excluded.nag_count,
                escalation_level = excluded.escalation_level,
                custom_nag_message = excluded.custom_nag_message,
                is_template = excluded.is_template,
                parent_template_id = excluded.parent_template_id,
                updated_at = excluded.updated_at;
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setTaskParameters(ps, task);
            ps.executeUpdate();
            return task;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to save task: " + task.getId(), e);
            throw new RuntimeException("Could not save task " + task.getId(), e);
        }
    }

    @Override
    public Optional<Task> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM tasks WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToTask(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to find task by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Task> findAll() {
        String sql = "SELECT * FROM tasks ORDER BY created_at DESC;";
        return executeQuery(sql);
    }

    @Override
    public List<Task> findActiveTasks() {
        String sql = """
            SELECT * FROM tasks
            WHERE is_template = 0 AND status NOT IN ('COMPLETED', 'DELETED')
            ORDER BY COALESCE(postponed_until, due_date_time) ASC;
        """;
        return executeQuery(sql);
    }

    @Override
    public List<Task> findTasksForDate(LocalDate date) {
        Objects.requireNonNull(date, "date cannot be null");
        String startStr = date.atStartOfDay().toString();
        String endStr = date.plusDays(1).atStartOfDay().toString();

        String sql = """
            SELECT * FROM tasks
            WHERE is_template = 0 AND status != 'DELETED'
              AND (
                  (due_date_time >= ? AND due_date_time < ?)
                  OR (postponed_until >= ? AND postponed_until < ?)
                  OR (status IN ('PENDING', 'OVERDUE') AND due_date_time < ?)
              )
            ORDER BY COALESCE(postponed_until, due_date_time) ASC;
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, startStr);
            ps.setString(2, endStr);
            ps.setString(3, startStr);
            ps.setString(4, endStr);
            ps.setString(5, startStr);
            try (ResultSet rs = ps.executeQuery()) {
                List<Task> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRowToTask(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to find tasks for date: " + date, e);
            return List.of();
        }
    }

    @Override
    public List<Task> findOverdueTasks(LocalDateTime referenceTime) {
        Objects.requireNonNull(referenceTime, "referenceTime cannot be null");
        String refStr = referenceTime.toString();
        String sql = """
            SELECT * FROM tasks
            WHERE is_template = 0 AND status NOT IN ('COMPLETED', 'DELETED')
              AND COALESCE(postponed_until, due_date_time) < ?
            ORDER BY COALESCE(postponed_until, due_date_time) ASC;
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, refStr);
            try (ResultSet rs = ps.executeQuery()) {
                List<Task> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRowToTask(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to find overdue tasks at: " + referenceTime, e);
            return List.of();
        }
    }

    @Override
    public List<Task> findRecurringTemplates() {
        String sql = """
            SELECT * FROM tasks
            WHERE is_template = 1 AND status != 'DELETED'
            ORDER BY title ASC;
        """;
        return executeQuery(sql);
    }

    @Override
    public boolean hasInstanceForTemplateOnDate(String templateId, LocalDate date) {
        if (templateId == null || date == null) {
            return false;
        }
        String startStr = date.atStartOfDay().toString();
        String endStr = date.plusDays(1).atStartOfDay().toString();

        String sql = """
            SELECT COUNT(1) FROM tasks
            WHERE parent_template_id = ?
              AND due_date_time >= ? AND due_date_time < ?
              AND status != 'DELETED';
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, templateId);
            ps.setString(2, startStr);
            ps.setString(3, endStr);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking instance for template " + templateId, e);
        }
        return false;
    }

    @Override
    public List<Task> searchAndFilter(String query, TaskCategory category, Priority priority, TaskStatus status) {
        StringBuilder sql = new StringBuilder("SELECT * FROM tasks WHERE is_template = 0 ");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            sql.append(" AND (LOWER(title) LIKE ? OR LOWER(description) LIKE ?) ");
            String q = "%" + query.trim().toLowerCase() + "%";
            params.add(q);
            params.add(q);
        }

        if (category != null) {
            sql.append(" AND category = ? ");
            params.add(category.name());
        }

        if (priority != null) {
            sql.append(" AND priority = ? ");
            params.add(priority.name());
        }

        if (status != null) {
            sql.append(" AND status = ? ");
            params.add(status.name());
        } else {
            // Default: do not show soft-deleted tasks unless explicitly requested
            sql.append(" AND status != 'DELETED' ");
        }

        sql.append(" ORDER BY COALESCE(postponed_until, due_date_time) ASC;");

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Task> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRowToTask(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Search query failed", e);
            return List.of();
        }
    }

    @Override
    public void delete(String id) {
        if (id == null) return;
        String sql = "UPDATE tasks SET status = 'DELETED', updated_at = ? WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Soft delete failed for id: " + id, e);
        }
    }

    @Override
    public void restore(String id) {
        if (id == null) return;
        String sql = "UPDATE tasks SET status = 'PENDING', updated_at = ? WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Restore failed for id: " + id, e);
        }
    }

    @Override
    public void hardDelete(String id) {
        if (id == null) return;
        String sql = "DELETE FROM tasks WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Hard delete failed for id: " + id, e);
        }
    }

    private List<Task> executeQuery(String sql) {
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<Task> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRowToTask(rs));
            }
            return list;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Query failed: " + sql, e);
            return List.of();
        }
    }

    private void setTaskParameters(PreparedStatement ps, Task task) throws SQLException {
        ps.setString(1, task.getId());
        ps.setString(2, task.getTitle());
        ps.setString(3, task.getDescription());
        ps.setString(4, task.getCategory().name());
        ps.setString(5, task.getPriority().name());
        ps.setString(6, task.getDueDateTime() != null ? task.getDueDateTime().toString() : null);

        RecurrenceRule rule = task.getRecurrenceRule();
        ps.setString(7, rule != null ? rule.getType().name() : RecurrenceType.NONE.name());
        ps.setString(8, (rule != null && rule.getDefaultTime() != null) ? rule.getDefaultTime().toString() : null);
        ps.setString(9, (rule != null && rule.getDayOfWeek() != null) ? rule.getDayOfWeek().name() : null);

        ps.setString(10, task.getStatus().name());
        ps.setString(11, task.getCompletedAt() != null ? task.getCompletedAt().toString() : null);
        ps.setString(12, task.getPostponedUntil() != null ? task.getPostponedUntil().toString() : null);
        ps.setInt(13, task.getNagCount());
        ps.setInt(14, task.getEscalationLevel().getLevel());
        ps.setString(15, task.getCustomNagMessage());
        ps.setInt(16, task.isTemplate() ? 1 : 0);
        ps.setString(17, task.getParentTemplateId());
        ps.setString(18, task.getCreatedAt().toString());
        ps.setString(19, task.getUpdatedAt().toString());
    }

    private Task mapRowToTask(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String title = rs.getString("title");
        String description = rs.getString("description");
        TaskCategory category = TaskCategory.fromString(rs.getString("category"));
        Priority priority = Priority.valueOf(rs.getString("priority"));

        String dueStr = rs.getString("due_date_time");
        LocalDateTime dueDateTime = dueStr != null ? LocalDateTime.parse(dueStr) : null;

        String recTypeStr = rs.getString("recurrence_type");
        RecurrenceType recType = recTypeStr != null ? RecurrenceType.valueOf(recTypeStr) : RecurrenceType.NONE;

        String recTimeStr = rs.getString("recurrence_time");
        LocalTime recTime = recTimeStr != null ? LocalTime.parse(recTimeStr) : LocalTime.of(12, 0);

        String recDowStr = rs.getString("recurrence_day_of_week");
        DayOfWeek dow = recDowStr != null ? DayOfWeek.valueOf(recDowStr) : null;

        RecurrenceRule rule = new RecurrenceRule(recType, recTime, dow);

        TaskStatus status = TaskStatus.valueOf(rs.getString("status"));

        String compStr = rs.getString("completed_at");
        LocalDateTime completedAt = compStr != null ? LocalDateTime.parse(compStr) : null;

        String postStr = rs.getString("postponed_until");
        LocalDateTime postponedUntil = postStr != null ? LocalDateTime.parse(postStr) : null;

        int nagCount = rs.getInt("nag_count");
        int escLevel = rs.getInt("escalation_level");
        String customMsg = rs.getString("custom_nag_message");
        boolean isTemplate = rs.getInt("is_template") == 1;
        String parentId = rs.getString("parent_template_id");

        LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"));
        LocalDateTime updatedAt = LocalDateTime.parse(rs.getString("updated_at"));

        return new Task(id, title, description, category, priority, dueDateTime, rule,
                status, completedAt, postponedUntil, nagCount, EscalationLevel.fromLevel(escLevel),
                customMsg, isTemplate, parentId, createdAt, updatedAt);
    }
}
