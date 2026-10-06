package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.StreakInfo;
import com.aarav.didyoudoit.model.Task;
import com.aarav.didyoudoit.repository.HistoryRepository;
import com.aarav.didyoudoit.util.ClockService;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementation of {@link StreakService}.
 * Implements FR-13, FR-14, FR-22.
 */
public class StreakServiceImpl implements StreakService {

    private final HistoryRepository historyRepository;
    private final ClockService clockService;

    public StreakServiceImpl(HistoryRepository historyRepository, ClockService clockService) {
        this.historyRepository = Objects.requireNonNull(historyRepository, "historyRepository cannot be null");
        this.clockService = Objects.requireNonNull(clockService, "clockService cannot be null");
    }

    @Override
    public StreakInfo getStreak(String templateId, String taskTitle) {
        Objects.requireNonNull(templateId, "templateId cannot be null");
        Optional<StreakInfo> existing = historyRepository.findStreak(templateId);
        return existing.orElseGet(() -> new StreakInfo(templateId, taskTitle, 0, 0, null));
    }

    @Override
    public void recordTaskCompletion(Task task) {
        Objects.requireNonNull(task, "task cannot be null");
        String templateId = task.getParentTemplateId();
        if (templateId == null || templateId.isBlank()) {
            return;
        }

        StreakInfo streak = getStreak(templateId, task.getTitle());
        LocalDate completionDate = task.getCompletedAt() != null
                ? task.getCompletedAt().toLocalDate()
                : clockService.today();

        streak.recordCompletion(completionDate);
        historyRepository.saveStreak(streak);
    }

    @Override
    public List<StreakInfo> getAllStreaks() {
        return historyRepository.findAllStreaks();
    }

    @Override
    public StreakStats getStreakStats() {
        long totalCompleted = historyRepository.countCompletedTasks();
        long totalOverdue = historyRepository.countOverdueCompletions();
        List<StreakInfo> all = historyRepository.findAllStreaks();

        int activeStreaks = 0;
        int longest = 0;
        LocalDate today = clockService.today();
        LocalDate yesterday = today.minusDays(1);

        for (StreakInfo s : all) {
            LocalDate lastDate = s.getLastCompletedDate();
            // A streak is currently active if completed today or yesterday
            if (lastDate != null && (lastDate.equals(today) || lastDate.equals(yesterday))) {
                if (s.getCurrentStreak() > 0) {
                    activeStreaks++;
                }
            }
            if (s.getBestStreak() > longest) {
                longest = s.getBestStreak();
            }
        }

        return new StreakStats(totalCompleted, totalOverdue, activeStreaks, longest);
    }
}
