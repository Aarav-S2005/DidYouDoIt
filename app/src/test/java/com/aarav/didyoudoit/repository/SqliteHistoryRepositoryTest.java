package com.aarav.didyoudoit.repository;

import com.aarav.didyoudoit.model.Priority;
import com.aarav.didyoudoit.model.StreakInfo;
import com.aarav.didyoudoit.model.TaskCategory;
import com.aarav.didyoudoit.model.TaskHistory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SqliteHistoryRepositoryTest {

    private Path tempDbPath;
    private DatabaseManager dbManager;
    private HistoryRepository historyRepository;

    @BeforeEach
    void setUp() throws IOException {
        tempDbPath = Files.createTempFile("didyoudoit_test_history_", ".db");
        dbManager = new SqliteDatabaseManager(tempDbPath.toString());
        historyRepository = new SqliteHistoryRepository(dbManager);
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
    @DisplayName("Log and retrieve completion history")
    void testLogCompletionAndCount() {
        TaskHistory h1 = new TaskHistory(
                "h1", "t1", "Morning Jog", TaskCategory.FITNESS, Priority.HIGH,
                "tpl1", LocalDateTime.now().minusHours(2), LocalDateTime.now(),
                false, 0, LocalDateTime.now()
        );
        TaskHistory h2 = new TaskHistory(
                "h2", "t2", "Read Book", TaskCategory.PERSONAL, Priority.LOW,
                null, LocalDateTime.now().minusHours(4), LocalDateTime.now(),
                true, 3, LocalDateTime.now()
        );

        historyRepository.logCompletion(h1);
        historyRepository.logCompletion(h2);

        assertEquals(2, historyRepository.countCompletedTasks());
        assertEquals(1, historyRepository.countOverdueCompletions());

        List<TaskHistory> recent = historyRepository.findRecentHistory(10);
        assertEquals(2, recent.size());
    }

    @Test
    @DisplayName("Record and update consecutive streaks")
    void testStreaks() {
        LocalDate day1 = LocalDate.of(2026, 10, 1);
        LocalDate day2 = LocalDate.of(2026, 10, 2);
        LocalDate day3 = LocalDate.of(2026, 10, 3);
        LocalDate day5 = LocalDate.of(2026, 10, 5); // Missed day 4!

        StreakInfo streak = new StreakInfo("habit-1", "Drink Water", 0, 0, null);
        streak.recordCompletion(day1);
        assertEquals(1, streak.getCurrentStreak());
        assertEquals(1, streak.getBestStreak());

        streak.recordCompletion(day2);
        assertEquals(2, streak.getCurrentStreak());

        streak.recordCompletion(day3);
        assertEquals(3, streak.getCurrentStreak());
        assertEquals(3, streak.getBestStreak());

        // Same day completion should be idempotent
        streak.recordCompletion(day3);
        assertEquals(3, streak.getCurrentStreak());

        // Missed day 4, completing day 5 resets current streak to 1 while preserving best streak
        streak.recordCompletion(day5);
        assertEquals(1, streak.getCurrentStreak());
        assertEquals(3, streak.getBestStreak());

        historyRepository.saveStreak(streak);

        Optional<StreakInfo> loaded = historyRepository.findStreak("habit-1");
        assertTrue(loaded.isPresent());
        assertEquals(1, loaded.get().getCurrentStreak());
        assertEquals(3, loaded.get().getBestStreak());
        assertEquals(day5, loaded.get().getLastCompletedDate());
    }
}
