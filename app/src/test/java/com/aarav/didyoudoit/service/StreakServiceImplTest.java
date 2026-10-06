package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.StreakInfo;
import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.repository.DatabaseManager;
import com.aarav.didyoudoit.repository.HistoryRepository;
import com.aarav.didyoudoit.repository.SqliteDatabaseManager;
import com.aarav.didyoudoit.repository.SqliteHistoryRepository;
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

class StreakServiceImplTest {

    private Path tempDbPath;
    private DatabaseManager dbManager;
    private HistoryRepository historyRepository;
    private TestClockService clockService;
    private StreakService streakService;

    @BeforeEach
    void setUp() throws IOException {
        tempDbPath = Files.createTempFile("didyoudoit_service_streak_", ".db");
        dbManager = new SqliteDatabaseManager(tempDbPath.toString());
        historyRepository = new SqliteHistoryRepository(dbManager);
        clockService = new TestClockService(LocalDateTime.of(2026, 10, 1, 10, 0));
        streakService = new StreakServiceImpl(historyRepository, clockService);
    }

    @AfterEach
    void tearDown() {
        dbManager.close();
        try {
            Files.deleteIfExists(tempDbPath);
        } catch (IOException ignored) {}
    }

    @Test
    @DisplayName("Streak increments across consecutive days and tracks best streak")
    void testConsecutiveStreak() {
        String templateId = "habit-meditate";

        // Day 1
        Task t1 = Task.builder()
                .title("Meditate")
                .parentTemplateId(templateId)
                .completedAt(clockService.now())
                .build();
        streakService.recordTaskCompletion(t1);

        StreakInfo s1 = streakService.getStreak(templateId, "Meditate");
        assertEquals(1, s1.getCurrentStreak());

        // Day 2
        clockService.advanceDays(1);
        Task t2 = Task.builder()
                .title("Meditate")
                .parentTemplateId(templateId)
                .completedAt(clockService.now())
                .build();
        streakService.recordTaskCompletion(t2);

        StreakInfo s2 = streakService.getStreak(templateId, "Meditate");
        assertEquals(2, s2.getCurrentStreak());
        assertEquals(2, s2.getBestStreak());

        // Skip 2 days (jump to Day 4)
        clockService.advanceDays(2);
        Task t4 = Task.builder()
                .title("Meditate")
                .parentTemplateId(templateId)
                .completedAt(clockService.now())
                .build();
        streakService.recordTaskCompletion(t4);

        StreakInfo s4 = streakService.getStreak(templateId, "Meditate");
        assertEquals(1, s4.getCurrentStreak(), "Current streak resets after skipped day");
        assertEquals(2, s4.getBestStreak(), "Best streak is preserved");
    }

    @Test
    @DisplayName("Streak statistics calculate total and active streaks correctly")
    void testStreakStats() {
        String templateId = "habit-water";
        Task t = Task.builder()
                .title("Water")
                .parentTemplateId(templateId)
                .completedAt(clockService.now())
                .build();
        streakService.recordTaskCompletion(t);

        StreakService.StreakStats stats = streakService.getStreakStats();
        assertEquals(1, stats.activeStreaksCount());
        assertEquals(1, stats.longestStreak());
    }
}
