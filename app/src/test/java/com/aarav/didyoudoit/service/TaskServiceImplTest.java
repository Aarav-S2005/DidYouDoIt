package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.*;
import com.aarav.didyoudoit.repository.*;
import com.aarav.didyoudoit.util.TestClockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TaskServiceImplTest {

    private Path tempDbPath;
    private DatabaseManager dbManager;
    private TaskRepository taskRepository;
    private HistoryRepository historyRepository;
    private TestClockService clockService;
    private StreakService streakService;
    private RecurringTaskEngine recurringTaskEngine;
    private TaskService taskService;

    @BeforeEach
    void setUp() throws IOException {
        tempDbPath = Files.createTempFile("didyoudoit_service_task_", ".db");
        dbManager = new SqliteDatabaseManager(tempDbPath.toString());
        taskRepository = new SqliteTaskRepository(dbManager);
        historyRepository = new SqliteHistoryRepository(dbManager);

        clockService = new TestClockService(LocalDateTime.of(2026, 10, 6, 9, 0));
        streakService = new StreakServiceImpl(historyRepository, clockService);
        recurringTaskEngine = new RecurringTaskEngineImpl(taskRepository, clockService);
        taskService = new TaskServiceImpl(taskRepository, historyRepository, streakService, recurringTaskEngine, clockService);
    }

    @AfterEach
    void tearDown() {
        dbManager.close();
        try {
            Files.deleteIfExists(tempDbPath);
        } catch (IOException ignored) {}
    }

    @Test
    @DisplayName("Create one-off task and retrieve today tasks")
    void testCreateOneOffTask() {
        Task task = Task.builder()
                .title("Write Documentation")
                .dueDateTime(LocalDateTime.of(2026, 10, 6, 15, 0))
                .priority(Priority.HIGH)
                .category(TaskCategory.WORK)
                .build();

        Task created = taskService.createTask(task);
        assertNotNull(created.getId());
        assertFalse(created.isTemplate());

        List<Task> todayTasks = taskService.getTodayTasks();
        assertEquals(1, todayTasks.size());
        assertEquals("Write Documentation", todayTasks.get(0).getTitle());
    }

    @Test
    @DisplayName("Create recurring task automatically generates today instance")
    void testCreateRecurringTask() {
        Task habit = Task.builder()
                .title("Drink 2L Water")
                .recurrenceRule(RecurrenceRule.daily(LocalTime.of(10, 0)))
                .category(TaskCategory.HEALTH)
                .build();

        taskService.createTask(habit);

        List<Task> todayTasks = taskService.getTodayTasks();
        assertEquals(1, todayTasks.size());
        Task todayInstance = todayTasks.get(0);
        assertEquals("Drink 2L Water", todayInstance.getTitle());
        assertFalse(todayInstance.isTemplate());
        assertNotNull(todayInstance.getParentTemplateId());

        // Template also exists in repository
        List<Task> templates = taskRepository.findRecurringTemplates();
        assertEquals(1, templates.size());
        assertTrue(templates.get(0).isTemplate());
    }

    @Test
    @DisplayName("Completing a task logs history and updates streak")
    void testCompleteTask() {
        Task habit = Task.builder()
                .title("Daily Exercise")
                .recurrenceRule(RecurrenceRule.daily(LocalTime.of(18, 0)))
                .build();

        taskService.createTask(habit);
        Task instance = taskService.getTodayTasks().get(0);

        taskService.completeTask(instance.getId());

        Task completed = taskService.getTask(instance.getId()).orElseThrow();
        assertEquals(TaskStatus.COMPLETED, completed.getStatus());
        assertNotNull(completed.getCompletedAt());

        assertEquals(1, historyRepository.countCompletedTasks());
        StreakInfo streak = streakService.getStreak(habit.getId(), habit.getTitle());
        assertEquals(1, streak.getCurrentStreak());
    }

    @Test
    @DisplayName("Postpone task updates status and postponedUntil")
    void testPostponeTask() {
        Task task = Task.builder()
                .title("Clean Desk")
                .dueDateTime(clockService.now().minusMinutes(5))
                .build();
        Task created = taskService.createTask(task);

        taskService.postponeTask(created.getId(), 30);

        Task postponed = taskService.getTask(created.getId()).orElseThrow();
        assertEquals(TaskStatus.POSTPONED, postponed.getStatus());
        assertEquals(clockService.now().plusMinutes(30), postponed.getPostponedUntil());
    }

    @Test
    @DisplayName("Record nag increments nagCount and updates overdue status")
    void testRecordNag() {
        Task task = Task.builder()
                .title("Submit Invoice")
                .dueDateTime(clockService.now().minusMinutes(10))
                .status(TaskStatus.PENDING)
                .build();
        Task created = taskService.createTask(task);

        taskService.recordNag(created.getId());

        Task nagged = taskService.getTask(created.getId()).orElseThrow();
        assertEquals(1, nagged.getNagCount());
        assertEquals(EscalationLevel.NUDGE, nagged.getEscalationLevel());
        assertEquals(TaskStatus.OVERDUE, nagged.getStatus());
    }
}
