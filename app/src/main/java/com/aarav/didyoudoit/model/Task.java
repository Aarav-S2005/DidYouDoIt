package com.aarav.didyoudoit.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Core Task entity representing scheduled user tasks and recurring task templates.
 * Implements FR-01, FR-02, FR-03, FR-09, FR-10, FR-11, FR-16.
 */
public class Task {

    private final String id;
    private String title;
    private String description;
    private TaskCategory category;
    private Priority priority;
    private LocalDateTime dueDateTime;
    private RecurrenceRule recurrenceRule;
    private TaskStatus status;
    private LocalDateTime completedAt;
    private LocalDateTime postponedUntil;
    private int nagCount;
    private EscalationLevel escalationLevel;
    private String customNagMessage;
    private boolean isTemplate;
    private String parentTemplateId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Task(String id,
                String title,
                String description,
                TaskCategory category,
                Priority priority,
                LocalDateTime dueDateTime,
                RecurrenceRule recurrenceRule,
                TaskStatus status,
                LocalDateTime completedAt,
                LocalDateTime postponedUntil,
                int nagCount,
                EscalationLevel escalationLevel,
                String customNagMessage,
                boolean isTemplate,
                String parentTemplateId,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
        this.id = Objects.requireNonNullElseGet(id, () -> UUID.randomUUID().toString());
        this.title = Objects.requireNonNullElse(title, "");
        this.description = Objects.requireNonNullElse(description, "");
        this.category = Objects.requireNonNullElse(category, TaskCategory.OTHER);
        this.priority = Objects.requireNonNullElse(priority, Priority.MEDIUM);
        this.dueDateTime = dueDateTime;
        this.recurrenceRule = Objects.requireNonNullElseGet(recurrenceRule, RecurrenceRule::none);
        this.status = Objects.requireNonNullElse(status, TaskStatus.PENDING);
        this.completedAt = completedAt;
        this.postponedUntil = postponedUntil;
        this.nagCount = Math.max(0, nagCount);
        this.escalationLevel = Objects.requireNonNullElse(escalationLevel, EscalationLevel.INITIAL);
        this.customNagMessage = customNagMessage;
        this.isTemplate = isTemplate;
        this.parentTemplateId = parentTemplateId;
        this.createdAt = Objects.requireNonNullElseGet(createdAt, LocalDateTime::now);
        this.updatedAt = Objects.requireNonNullElseGet(updatedAt, LocalDateTime::now);
    }

    public static Builder builder() {
        return new Builder();
    }

    // --- Getters and Mutators ---

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = Objects.requireNonNullElse(title, "");
        this.updatedAt = LocalDateTime.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = Objects.requireNonNullElse(description, "");
        this.updatedAt = LocalDateTime.now();
    }

    public TaskCategory getCategory() {
        return category;
    }

    public void setCategory(TaskCategory category) {
        this.category = Objects.requireNonNullElse(category, TaskCategory.OTHER);
        this.updatedAt = LocalDateTime.now();
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = Objects.requireNonNullElse(priority, Priority.MEDIUM);
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getDueDateTime() {
        return dueDateTime;
    }

    public void setDueDateTime(LocalDateTime dueDateTime) {
        this.dueDateTime = dueDateTime;
        this.updatedAt = LocalDateTime.now();
    }

    public RecurrenceRule getRecurrenceRule() {
        return recurrenceRule;
    }

    public void setRecurrenceRule(RecurrenceRule recurrenceRule) {
        this.recurrenceRule = Objects.requireNonNullElseGet(recurrenceRule, RecurrenceRule::none);
        this.updatedAt = LocalDateTime.now();
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = Objects.requireNonNullElse(status, TaskStatus.PENDING);
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getPostponedUntil() {
        return postponedUntil;
    }

    public void setPostponedUntil(LocalDateTime postponedUntil) {
        this.postponedUntil = postponedUntil;
        this.updatedAt = LocalDateTime.now();
    }

    public int getNagCount() {
        return nagCount;
    }

    public void setNagCount(int nagCount) {
        this.nagCount = Math.max(0, nagCount);
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementNagCount() {
        this.nagCount++;
        this.escalationLevel = this.escalationLevel.next();
        this.updatedAt = LocalDateTime.now();
    }

    public EscalationLevel getEscalationLevel() {
        return escalationLevel;
    }

    public void setEscalationLevel(EscalationLevel escalationLevel) {
        this.escalationLevel = Objects.requireNonNullElse(escalationLevel, EscalationLevel.INITIAL);
        this.updatedAt = LocalDateTime.now();
    }

    public String getCustomNagMessage() {
        return customNagMessage;
    }

    public void setCustomNagMessage(String customNagMessage) {
        this.customNagMessage = customNagMessage;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isTemplate() {
        return isTemplate;
    }

    public void setTemplate(boolean template) {
        isTemplate = template;
        this.updatedAt = LocalDateTime.now();
    }

    public String getParentTemplateId() {
        return parentTemplateId;
    }

    public void setParentTemplateId(String parentTemplateId) {
        this.parentTemplateId = parentTemplateId;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isOverdue(LocalDateTime now) {
        if (status == TaskStatus.COMPLETED || status == TaskStatus.DELETED || dueDateTime == null) {
            return false;
        }
        if (postponedUntil != null) {
            return now.isAfter(postponedUntil);
        }
        return now.isAfter(dueDateTime);
    }

    public LocalDateTime getEffectiveDueDateTime() {
        return postponedUntil != null ? postponedUntil : dueDateTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task task)) return false;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // --- Builder Pattern ---

    public static class Builder {
        private String id;
        private String title = "";
        private String description = "";
        private TaskCategory category = TaskCategory.OTHER;
        private Priority priority = Priority.MEDIUM;
        private LocalDateTime dueDateTime;
        private RecurrenceRule recurrenceRule = RecurrenceRule.none();
        private TaskStatus status = TaskStatus.PENDING;
        private LocalDateTime completedAt;
        private LocalDateTime postponedUntil;
        private int nagCount = 0;
        private EscalationLevel escalationLevel = EscalationLevel.INITIAL;
        private String customNagMessage;
        private boolean isTemplate = false;
        private String parentTemplateId;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt = LocalDateTime.now();

        public Builder id(String id) { this.id = id; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder category(TaskCategory category) { this.category = category; return this; }
        public Builder priority(Priority priority) { this.priority = priority; return this; }
        public Builder dueDateTime(LocalDateTime dueDateTime) { this.dueDateTime = dueDateTime; return this; }
        public Builder recurrenceRule(RecurrenceRule rule) { this.recurrenceRule = rule; return this; }
        public Builder status(TaskStatus status) { this.status = status; return this; }
        public Builder completedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }
        public Builder postponedUntil(LocalDateTime postponedUntil) { this.postponedUntil = postponedUntil; return this; }
        public Builder nagCount(int nagCount) { this.nagCount = nagCount; return this; }
        public Builder escalationLevel(EscalationLevel level) { this.escalationLevel = level; return this; }
        public Builder customNagMessage(String msg) { this.customNagMessage = msg; return this; }
        public Builder isTemplate(boolean isTemplate) { this.isTemplate = isTemplate; return this; }
        public Builder parentTemplateId(String parentId) { this.parentTemplateId = parentId; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Task build() {
            return new Task(id, title, description, category, priority, dueDateTime, recurrenceRule,
                    status, completedAt, postponedUntil, nagCount, escalationLevel,
                    customNagMessage, isTemplate, parentTemplateId, createdAt, updatedAt);
        }
    }
}
