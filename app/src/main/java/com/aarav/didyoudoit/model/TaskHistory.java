package com.aarav.didyoudoit.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Historical record of completed or overdue task events for analytics and streaks.
 * Implements FR-13, FR-14.
 */
public class TaskHistory {

    private final String id;
    private final String taskId;
    private final String taskTitle;
    private final TaskCategory category;
    private final Priority priority;
    private final String parentTemplateId;
    private final LocalDateTime scheduledFor;
    private final LocalDateTime completedAt;
    private final boolean wasOverdue;
    private final int nagCount;
    private final LocalDateTime recordedAt;

    public TaskHistory(String id,
                       String taskId,
                       String taskTitle,
                       TaskCategory category,
                       Priority priority,
                       String parentTemplateId,
                       LocalDateTime scheduledFor,
                       LocalDateTime completedAt,
                       boolean wasOverdue,
                       int nagCount,
                       LocalDateTime recordedAt) {
        this.id = Objects.requireNonNullElseGet(id, () -> UUID.randomUUID().toString());
        this.taskId = taskId;
        this.taskTitle = Objects.requireNonNullElse(taskTitle, "");
        this.category = Objects.requireNonNullElse(category, TaskCategory.OTHER);
        this.priority = Objects.requireNonNullElse(priority, Priority.MEDIUM);
        this.parentTemplateId = parentTemplateId;
        this.scheduledFor = scheduledFor;
        this.completedAt = completedAt;
        this.wasOverdue = wasOverdue;
        this.nagCount = nagCount;
        this.recordedAt = Objects.requireNonNullElseGet(recordedAt, LocalDateTime::now);
    }

    public static TaskHistory fromCompletedTask(Task task) {
        Objects.requireNonNull(task, "task cannot be null");
        boolean overdue = task.isOverdue(task.getCompletedAt() != null ? task.getCompletedAt() : LocalDateTime.now());
        return new TaskHistory(
                UUID.randomUUID().toString(),
                task.getId(),
                task.getTitle(),
                task.getCategory(),
                task.getPriority(),
                task.getParentTemplateId(),
                task.getDueDateTime(),
                task.getCompletedAt() != null ? task.getCompletedAt() : LocalDateTime.now(),
                overdue,
                task.getNagCount(),
                LocalDateTime.now()
        );
    }

    public String getId() {
        return id;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public TaskCategory getCategory() {
        return category;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getParentTemplateId() {
        return parentTemplateId;
    }

    public LocalDateTime getScheduledFor() {
        return scheduledFor;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public boolean isWasOverdue() {
        return wasOverdue;
    }

    public int getNagCount() {
        return nagCount;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }
}
