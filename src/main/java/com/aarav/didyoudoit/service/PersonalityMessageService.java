package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.EscalationLevel;
import com.aarav.didyoudoit.model.PersonalityType;
import com.aarav.didyoudoit.model.Task;

/**
 * Generates context-aware reminder and nagging messages based on task urgency,
 * minutes overdue, and user personality configuration.
 * Implements FR-06, FR-07, FR-15, FR-16, BR-02, BR-12.
 */
public interface PersonalityMessageService {

    /**
     * Represents a formatted nagging notification message.
     *
     * @param title short attention-grabbing headline
     * @param body detailed context-aware nagging text
     * @param level escalation level
     */
    record NagMessage(String title, String body, EscalationLevel level) {}

    /**
     * Generates a nagging message for a task given a personality style and lateness.
     *
     * @param task the due or overdue task
     * @param personality active personality tone
     * @param minutesOverdue minutes past the scheduled deadline
     * @return formatted {@link NagMessage}
     */
    NagMessage generateMessage(Task task, PersonalityType personality, int minutesOverdue);
}
