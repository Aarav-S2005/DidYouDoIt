package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.RecurrenceRule;
import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.model.TaskStatus;
import com.aarav.didyoudoit.repository.TaskRepository;
import com.aarav.didyoudoit.util.ClockService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Implementation of {@link RecurringTaskEngine}.
 * Implements FR-03, FR-12.
 */
public class RecurringTaskEngineImpl implements RecurringTaskEngine {

    private static final Logger LOGGER = Logger.getLogger(RecurringTaskEngineImpl.class.getName());

    private final TaskRepository taskRepository;
    private final ClockService clockService;

    public RecurringTaskEngineImpl(TaskRepository taskRepository, ClockService clockService) {
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository cannot be null");
        this.clockService = Objects.requireNonNull(clockService, "clockService cannot be null");
    }

    @Override
    public List<Task> generateDailyInstances(LocalDate targetDate) {
        LocalDate date = (targetDate != null) ? targetDate : clockService.today();
        List<Task> templates = taskRepository.findRecurringTemplates();
        List<Task> createdInstances = new ArrayList<>();

        for (Task template : templates) {
            RecurrenceRule rule = template.getRecurrenceRule();
            if (rule != null && rule.appliesToDate(date)) {
                boolean alreadyExists = taskRepository.hasInstanceForTemplateOnDate(template.getId(), date);
                if (!alreadyExists) {
                    Task instance = createInstanceFromTemplate(template, date);
                    taskRepository.save(instance);
                    createdInstances.add(instance);
                    LOGGER.info("Generated daily task instance '" + instance.getTitle() + "' for date " + date);
                }
            }
        }

        return createdInstances;
    }

    @Override
    public Task createInstanceFromTemplate(Task template, LocalDate targetDate) {
        Objects.requireNonNull(template, "template cannot be null");
        Objects.requireNonNull(targetDate, "targetDate cannot be null");

        LocalTime time = (template.getRecurrenceRule() != null && template.getRecurrenceRule().getDefaultTime() != null)
                ? template.getRecurrenceRule().getDefaultTime()
                : LocalTime.of(12, 0);

        LocalDateTime dueDateTime = LocalDateTime.of(targetDate, time);

        return Task.builder()
                .id(UUID.randomUUID().toString())
                .title(template.getTitle())
                .description(template.getDescription())
                .category(template.getCategory())
                .priority(template.getPriority())
                .dueDateTime(dueDateTime)
                .recurrenceRule(RecurrenceRule.none())
                .status(TaskStatus.PENDING)
                .customNagMessage(template.getCustomNagMessage())
                .isTemplate(false)
                .parentTemplateId(template.getId())
                .createdAt(clockService.now())
                .updatedAt(clockService.now())
                .build();
    }
}
