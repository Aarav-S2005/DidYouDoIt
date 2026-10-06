package com.aarav.didyoudoit.repository;

import com.aarav.didyoudoit.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SqliteTaskRepositoryTest {

    private Path tempDbPath;
    private DatabaseManager dbManager;
    private TaskRepository taskRepository;

    @BeforeEach
    void setUp() throws IOException {
        tempDbPath = Files.createTempFile("didyoudoit_test_tasks_", ".db");
        dbManager = new SqliteDatabaseManager(tempDbPath.toString());
        taskRepository = new SqliteTaskRepository(dbManager);
    }

    @AfterEach
    void tearDown() {
        dbManager.close();
        try {
            Files.deleteIfExists(tempDbPath);
        } catch (IOException ignored) {
        }
    }

    @Test
    @DisplayName("Save and find task by ID")
    void testSaveAndFindById() {
        Task task = Task.builder()
                .title("Complete Assignment")
                .description("Math exercises 1 to 10")
                .category(TaskCategory.STUDY)
                .priority(Priority.HIGH)
                .dueDateTime(LocalDateTime.of(2026, 10, 6, 17, 0))
                .status(TaskStatus.PENDING)
                .build();

        taskRepository.save(task);

        Optional<Task> loaded = taskRepository.findById(task.getId());
        assertTrue(loaded.isPresent());
        assertEquals("Complete Assignment", loaded.get().getTitle());
        assertEquals("Math exercises 1 to 10", loaded.get().getDescription());
        assertEquals(TaskCategory.STUDY, loaded.get().getCategory());
        assertEquals(Priority.HIGH, loaded.get().getPriority());
        assertEquals(TaskStatus.PENDING, loaded.get().getStatus());
    }

    @Test
    @DisplayName("Update existing task via save")
    void testUpdateTask() {
        Task task = Task.builder()
                .title("Go to Gym")
                .category(TaskCategory.FITNESS)
                .priority(Priority.MEDIUM)
                .dueDateTime(LocalDateTime.of(2026, 10, 6, 18, 0))
                .build();

        taskRepository.save(task);

        task.setTitle("Go to Gym - Leg Day");
        task.setPriority(Priority.URGENT);
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.of(2026, 10, 6, 19, 0));

        taskRepository.save(task);

        Optional<Task> updated = taskRepository.findById(task.getId());
        assertTrue(updated.isPresent());
        assertEquals("Go to Gym - Leg Day", updated.get().getTitle());
        assertEquals(Priority.URGENT, updated.get().getPriority());
        assertEquals(TaskStatus.COMPLETED, updated.get().getStatus());
        assertNotNull(updated.get().getCompletedAt());
    }

    @Test
    @DisplayName("Soft delete and restore task")
    void testDeleteAndRestore() {
        Task task = Task.builder()
                .title("Temporary task")
                .build();
        taskRepository.save(task);

        taskRepository.delete(task.getId());
        Optional<Task> deleted = taskRepository.findById(task.getId());
        assertTrue(deleted.isPresent());
        assertEquals(TaskStatus.DELETED, deleted.get().getStatus());

        taskRepository.restore(task.getId());
        Optional<Task> restored = taskRepository.findById(task.getId());
        assertTrue(restored.isPresent());
        assertEquals(TaskStatus.PENDING, restored.get().getStatus());
    }

    @Test
    @DisplayName("Find active tasks excludes completed and deleted")
    void testFindActiveTasks() {
        Task active1 = Task.builder().title("Task 1").status(TaskStatus.PENDING).build();
        Task active2 = Task.builder().title("Task 2").status(TaskStatus.POSTPONED).build();
        Task completed = Task.builder().title("Task 3").status(TaskStatus.COMPLETED).build();
        Task template = Task.builder().title("Template").isTemplate(true).build();

        taskRepository.save(active1);
        taskRepository.save(active2);
        taskRepository.save(completed);
        taskRepository.save(template);

        List<Task> activeList = taskRepository.findActiveTasks();
        assertEquals(2, activeList.size());
    }

    @Test
    @DisplayName("Find tasks for specific date")
    void testFindTasksForDate() {
        LocalDate today = LocalDate.of(2026, 10, 6);
        Task todayTask = Task.builder()
                .title("Today's Task")
                .dueDateTime(LocalDateTime.of(today, LocalTime.of(14, 0)))
                .status(TaskStatus.PENDING)
                .build();

        Task tomorrowTask = Task.builder()
                .title("Tomorrow's Task")
                .dueDateTime(LocalDateTime.of(today.plusDays(1), LocalTime.of(10, 0)))
                .status(TaskStatus.PENDING)
                .build();

        taskRepository.save(todayTask);
        taskRepository.save(tomorrowTask);

        List<Task> result = taskRepository.findTasksForDate(today);
        assertEquals(1, result.size());
        assertEquals("Today's Task", result.get(0).getTitle());
    }

    @Test
    @DisplayName("Search and filtering query")
    void testSearchAndFilter() {
        Task t1 = Task.builder().title("Study Algorithms").category(TaskCategory.STUDY).priority(Priority.HIGH).build();
        Task t2 = Task.builder().title("Study Physics").category(TaskCategory.STUDY).priority(Priority.LOW).build();
        Task t3 = Task.builder().title("Grocery Shopping").category(TaskCategory.PERSONAL).priority(Priority.LOW).build();

        taskRepository.save(t1);
        taskRepository.save(t2);
        taskRepository.save(t3);

        List<Task> searchStudy = taskRepository.searchAndFilter("study", null, null, null);
        assertEquals(2, searchStudy.size());

        List<Task> searchHigh = taskRepository.searchAndFilter(null, null, Priority.HIGH, null);
        assertEquals(1, searchHigh.size());
        assertEquals("Study Algorithms", searchHigh.get(0).getTitle());
    }

    @Test
    @DisplayName("Recurring template management and instance detection")
    void testRecurringTemplates() {
        Task template = Task.builder()
                .title("Morning Routine")
                .isTemplate(true)
                .recurrenceRule(RecurrenceRule.daily(LocalTime.of(8, 0)))
                .category(TaskCategory.PERSONAL)
                .build();

        taskRepository.save(template);

        List<Task> templates = taskRepository.findRecurringTemplates();
        assertEquals(1, templates.size());
        assertEquals("Morning Routine", templates.get(0).getTitle());

        LocalDate today = LocalDate.of(2026, 10, 6);
        assertFalse(taskRepository.hasInstanceForTemplateOnDate(template.getId(), today));

        Task dailyInstance = Task.builder()
                .title("Morning Routine")
                .parentTemplateId(template.getId())
                .dueDateTime(LocalDateTime.of(today, LocalTime.of(8, 0)))
                .isTemplate(false)
                .build();
        taskRepository.save(dailyInstance);

        assertTrue(taskRepository.hasInstanceForTemplateOnDate(template.getId(), today));
    }
}
