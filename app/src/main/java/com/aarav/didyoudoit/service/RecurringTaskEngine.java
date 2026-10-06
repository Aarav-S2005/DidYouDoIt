package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.Task;

import java.time.LocalDate;
import java.util.List;

/**
 * Automatically evaluates recurring task templates and generates concrete daily instances.
 * Implements FR-03, FR-12.
 */
public interface RecurringTaskEngine {

    /**
     * Generates and persists concrete task instances for all recurring templates applicable to the given date.
     * Prevents duplicate instances for the same day.
     *
     * @param targetDate date for which to generate tasks
     * @return newly generated task instances
     */
    List<Task> generateDailyInstances(LocalDate targetDate);

    /**
     * Spawns a concrete task instance from a template definition for a target date without persisting it yet.
     */
    Task createInstanceFromTemplate(Task template, LocalDate targetDate);
}
