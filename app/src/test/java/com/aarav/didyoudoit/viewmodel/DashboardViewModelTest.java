package com.aarav.didyoudoit.viewmodel;

import com.aarav.didyoudoit.model.Priority;
import com.aarav.didyoudoit.model.RecurrenceRule;
import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.model.TaskCategory;
import com.aarav.didyoudoit.model.TaskStatus;
import com.aarav.didyoudoit.repository.*;
import com.aarav.didyoudoit.service.*;
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

import static org.junit.jupiter.api.Assertions.*;

class DashboardViewModelTest {

    private Path tempDbPath;
    private DatabaseManager dbManager;
    private TaskRepository taskRepository;
    private HistoryRepository historyRepository;
    private TestClockService clockService;
    private StreakService streakService;
    private RecurringTaskEngine recurringEngine;
    private TaskService taskService;
    private DashboardViewModel viewModel;

    @BeforeEach
    void setUp() throws IOException {
        tempDbPath = Files.createTempFile("didyoudoit_vm_test_", ".db");
        dbManager = new SqliteDatabaseManager(tempDbPath.toString());
        taskRepository = new SqliteTaskRepository(dbManager);
        historyRepository = new SqliteHistoryRepository(dbManager);
        clockService = new TestClockService(LocalDateTime.of(2026, 10, 6, 12, 0));
        streakService = new StreakServiceImpl(historyRepository, clockService);
        recurringEngine = new RecurringTaskEngineImpl(taskRepository, clockService);
        taskService = new TaskServiceImpl(taskRepository, historyRepository, streakService, recurringEngine, clockService);

        viewModel = new DashboardViewModel(taskService, recurringEngine, streakService);
    }

    @AfterEach
    void tearDown() {
        dbManager.close();
        try {
            Files.deleteIfExists(tempDbPath);
        } catch (IOException ignored) {}
    }

    @Test
    @DisplayName("Reload tasks and apply search query filtering")
    void testSearchAndFiltering() {
        Task t1 = Task.builder()
                .title("Complete Physics Lab")
                .category(TaskCategory.STUDY)
                .priority(Priority.HIGH)
                .dueDateTime(clockService.now().plusHours(2))
                .build();
        Task t2 = Task.builder()
                .title("Clean Kitchen")
                .category(TaskCategory.CHORES)
                .priority(Priority.LOW)
                .dueDateTime(clockService.now().plusHours(3))
                .build();

        taskService.createTask(t1);
        taskService.createTask(t2);

        // Reload tasks synchronously for testing
        viewModel.reloadTasks().join();

        assertEquals(2, viewModel.getDisplayedTasks().size());

        // Search "physics"
        viewModel.searchQueryProperty().set("physics");
        assertEquals(1, viewModel.getDisplayedTasks().size());
        assertEquals("Complete Physics Lab", viewModel.getDisplayedTasks().get(0).getTitle());

        // Clear search, filter category CHORES
        viewModel.searchQueryProperty().set("");
        viewModel.selectedCategoryFilterProperty().set(TaskCategory.CHORES);
        assertEquals(1, viewModel.getDisplayedTasks().size());
        assertEquals("Clean Kitchen", viewModel.getDisplayedTasks().get(0).getTitle());
    }

    @Test
    @DisplayName("Toggle complete changes status and triggers history logging")
    void testToggleComplete() {
        Task t = Task.builder()
                .title("Submit Tax")
                .dueDateTime(clockService.now().plusHours(1))
                .build();
        Task saved = taskService.createTask(t);
        viewModel.reloadTasks().join();

        assertEquals(TaskStatus.PENDING, saved.getStatus());

        viewModel.toggleComplete(saved).join();

        Task completed = taskService.getTask(saved.getId()).orElseThrow();
        assertEquals(TaskStatus.COMPLETED, completed.getStatus());
        assertEquals(1, historyRepository.countCompletedTasks());
    }

    @Test
    @DisplayName("Snooze task updates postponed time")
    void testSnoozeTask() {
        Task t = Task.builder()
                .title("Gym Workout")
                .dueDateTime(clockService.now().minusMinutes(5))
                .build();
        Task saved = taskService.createTask(t);
        viewModel.reloadTasks().join();

        viewModel.snoozeTask(saved, 30).join();

        Task snoozed = taskService.getTask(saved.getId()).orElseThrow();
        assertEquals(TaskStatus.POSTPONED, snoozed.getStatus());
        assertNotNull(snoozed.getPostponedUntil());
    }

    @Test
    @DisplayName("Delete task removes from active list")
    void testDeleteTask() {
        Task t = Task.builder()
                .title("Disposable Note")
                .dueDateTime(clockService.now().plusHours(1))
                .build();
        Task saved = taskService.createTask(t);
        viewModel.reloadTasks().join();

        assertEquals(1, viewModel.getDisplayedTasks().size());

        viewModel.deleteTask(saved).join();

        assertEquals(0, viewModel.getDisplayedTasks().size());
    }

    @Test
    @DisplayName("Delete recurring task does not recreate on reload")
    void testDeleteRecurringTaskDoesNotRecreate() {
        Task habit = Task.builder()
                .title("Drink 2L Water")
                .recurrenceRule(RecurrenceRule.daily(LocalTime.of(9, 0)))
                .build();
        taskService.createTask(habit);

        viewModel.reloadTasks().join();
        assertEquals(1, viewModel.getDisplayedTasks().size());

        Task instance = viewModel.getDisplayedTasks().get(0);
        assertNotNull(instance.getParentTemplateId());

        viewModel.deleteTask(instance).join();
        assertEquals(0, viewModel.getDisplayedTasks().size(), "Task must be deleted immediately");

        // Reload tasks again: must not resurrect or recreate instance
        viewModel.reloadTasks().join();
        assertEquals(0, viewModel.getDisplayedTasks().size(), "Task must NOT reappear upon subsequent reload");
    }
}
