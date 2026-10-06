package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.RecurrenceRule;
import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.repository.DatabaseManager;
import com.aarav.didyoudoit.repository.SqliteDatabaseManager;
import com.aarav.didyoudoit.repository.SqliteTaskRepository;
import com.aarav.didyoudoit.repository.TaskRepository;
import com.aarav.didyoudoit.util.TestClockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecurringTaskEngineImplTest {

    private Path tempDbPath;
    private DatabaseManager dbManager;
    private TaskRepository taskRepository;
    private TestClockService clockService;
    private RecurringTaskEngine recurringEngine;

    @BeforeEach
    void setUp() throws IOException {
        tempDbPath = Files.createTempFile("didyoudoit_service_recurring_", ".db");
        dbManager = new SqliteDatabaseManager(tempDbPath.toString());
        taskRepository = new SqliteTaskRepository(dbManager);
        // Monday: 2026-10-05
        clockService = new TestClockService(LocalDateTime.of(2026, 10, 5, 8, 0));
        recurringEngine = new RecurringTaskEngineImpl(taskRepository, clockService);
    }

    @AfterEach
    void tearDown() {
        dbManager.close();
        try {
            Files.deleteIfExists(tempDbPath);
        } catch (IOException ignored) {}
    }

    @Test
    @DisplayName("Daily recurring task generates instance and does not duplicate on second run")
    void testDailyGenerationAndDeduplication() {
        Task dailyHabit = Task.builder()
                .title("Read 15 Pages")
                .isTemplate(true)
                .recurrenceRule(RecurrenceRule.daily(LocalTime.of(21, 0)))
                .build();
        taskRepository.save(dailyHabit);

        LocalDate today = clockService.today();

        // First generation
        List<Task> generated1 = recurringEngine.generateDailyInstances(today);
        assertEquals(1, generated1.size());
        assertEquals("Read 15 Pages", generated1.get(0).getTitle());
        assertEquals(dailyHabit.getId(), generated1.get(0).getParentTemplateId());

        // Second generation on same day should not generate duplicate
        List<Task> generated2 = recurringEngine.generateDailyInstances(today);
        assertEquals(0, generated2.size());
    }

    @Test
    @DisplayName("Weekdays rule generates on Monday but skips Sunday")
    void testWeekdayRule() {
        Task weekdayHabit = Task.builder()
                .title("Daily Standup")
                .isTemplate(true)
                .recurrenceRule(RecurrenceRule.weekdays(LocalTime.of(9, 30)))
                .build();
        taskRepository.save(weekdayHabit);

        // 2026-10-05 is Monday -> should generate
        List<Task> monGenerated = recurringEngine.generateDailyInstances(clockService.today());
        assertEquals(1, monGenerated.size());

        // 2026-10-11 is Sunday -> should not generate
        LocalDate sunday = LocalDate.of(2026, 10, 11);
        List<Task> sunGenerated = recurringEngine.generateDailyInstances(sunday);
        assertEquals(0, sunGenerated.size());
    }
}
