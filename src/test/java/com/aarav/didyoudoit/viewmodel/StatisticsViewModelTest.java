package com.aarav.didyoudoit.viewmodel;

import com.aarav.didyoudoit.model.Priority;
import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.model.TaskCategory;
import com.aarav.didyoudoit.model.TaskHistory;
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

import static org.junit.jupiter.api.Assertions.*;

class StatisticsViewModelTest {

    private Path tempDbPath;
    private DatabaseManager dbManager;
    private TaskRepository taskRepository;
    private HistoryRepository historyRepository;
    private TestClockService clockService;
    private StreakService streakService;
    private RecurringTaskEngine recurringEngine;
    private TaskService taskService;
    private StatisticsViewModel viewModel;

    @BeforeEach
    void setUp() throws IOException {
        tempDbPath = Files.createTempFile("didyoudoit_stats_test_", ".db");
        dbManager = new SqliteDatabaseManager(tempDbPath.toString());
        taskRepository = new SqliteTaskRepository(dbManager);
        historyRepository = new SqliteHistoryRepository(dbManager);
        clockService = new TestClockService(LocalDateTime.of(2026, 10, 6, 12, 0));
        streakService = new StreakServiceImpl(historyRepository, clockService);
        recurringEngine = new RecurringTaskEngineImpl(taskRepository, clockService);
        taskService = new TaskServiceImpl(taskRepository, historyRepository, streakService, recurringEngine, clockService);

        viewModel = new StatisticsViewModel(taskService, streakService, historyRepository, clockService);
    }

    @AfterEach
    void tearDown() {
        dbManager.close();
        try {
            Files.deleteIfExists(tempDbPath);
        } catch (IOException ignored) {}
    }

    @Test
    @DisplayName("Reload stats computes 100% completion rate when all tasks completed")
    void testReloadStats_CalculatesCompletionRate() {
        Task t1 = Task.builder()
                .title("Submit Expense Report")
                .category(TaskCategory.WORK)
                .priority(Priority.HIGH)
                .dueDateTime(LocalDateTime.of(2026, 10, 6, 10, 0))
                .build();
        Task created = taskService.createTask(t1);

        // Complete the task
        taskService.completeTask(created.getId());

        viewModel.reloadStats().join();

        assertEquals("100%", viewModel.completionRate7DaysProperty().get());
        assertEquals(1, viewModel.totalCompletedProperty().get());
        assertEquals(1, viewModel.getHistoryLogList().size());
        assertEquals("Submit Expense Report", viewModel.getHistoryLogList().get(0).getTaskTitle());
    }

    @Test
    @DisplayName("Reload stats reflects lower completion rate when overdue tasks exist")
    void testReloadStats_WithOverdueTasks() {
        // 1 completed task
        Task doneTask = Task.builder()
                .title("Morning Routine")
                .dueDateTime(LocalDateTime.of(2026, 10, 6, 8, 0))
                .build();
        Task savedDone = taskService.createTask(doneTask);
        taskService.completeTask(savedDone.getId());

        // 1 overdue task
        Task overdueTask = Task.builder()
                .title("Pay Electric Bill")
                .dueDateTime(LocalDateTime.of(2026, 10, 6, 9, 0))
                .build();
        taskService.createTask(overdueTask);

        viewModel.reloadStats().join();

        // 1 completed out of 2 = 50%
        assertEquals("50%", viewModel.completionRate7DaysProperty().get());
        assertEquals(1, viewModel.totalCompletedProperty().get());
        assertEquals(1, viewModel.missedCountThisWeekProperty().get());
    }

    @Test
    @DisplayName("Undo completion reopens task and removes history log record (FR-14)")
    void testUndoCompletion_ReopensTask() {
        Task t = Task.builder()
                .title("Read Chapter 5")
                .category(TaskCategory.STUDY)
                .dueDateTime(LocalDateTime.of(2026, 10, 6, 11, 0))
                .build();
        Task created = taskService.createTask(t);
        taskService.completeTask(created.getId());

        viewModel.reloadStats().join();
        assertEquals(1, viewModel.getHistoryLogList().size());

        TaskHistory entry = viewModel.getHistoryLogList().get(0);

        // Undo completion
        viewModel.undoCompletion(entry).join();

        // Verify history list is now empty
        assertEquals(0, viewModel.getHistoryLogList().size());

        // Verify task is back to PENDING in database
        Task reloaded = taskService.getTask(created.getId()).orElseThrow();
        assertEquals(TaskStatus.PENDING, reloaded.getStatus());
        assertNull(reloaded.getCompletedAt());
    }

    @Test
    @DisplayName("Accountability alert triggers for repeated overdue tasks (FR-22)")
    void testAccountabilityAlert_Triggered() {
        // Create 3 overdue tasks with the same title to simulate repeated misses
        for (int i = 0; i < 3; i++) {
            Task t = Task.builder()
                    .title("Gym Workout")
                    .dueDateTime(LocalDateTime.of(2026, 10, 5, 10, 0))
                    .build();
            taskService.createTask(t);
        }

        viewModel.reloadStats().join();

        assertTrue(viewModel.missedHabitsAlertProperty().get().contains("Gym Workout"));
        assertTrue(viewModel.missedHabitsAlertProperty().get().contains("3 times"));
    }
}
