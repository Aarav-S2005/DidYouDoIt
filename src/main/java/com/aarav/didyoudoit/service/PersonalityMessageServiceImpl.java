package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.EscalationLevel;
import com.aarav.didyoudoit.model.PersonalityType;
import com.aarav.didyoudoit.model.Task;

import java.util.Objects;

/**
 * Implementation of {@link PersonalityMessageService} containing curated nagging copy
 * across Gentle, Strict, Sarcastic, and Aggressive styles.
 * Implements FR-06, FR-07, FR-15, FR-16, BR-02, BR-12.
 */
public class PersonalityMessageServiceImpl implements PersonalityMessageService {

    @Override
    public NagMessage generateMessage(Task task, PersonalityType personality, int minutesOverdue) {
        Objects.requireNonNull(task, "task cannot be null");
        PersonalityType activePersonality = Objects.requireNonNullElse(personality, PersonalityType.SARCASTIC);

        EscalationLevel level = calculateEscalationLevel(minutesOverdue, task.getNagCount());
        String customMsg = task.getCustomNagMessage();

        if (customMsg != null && !customMsg.isBlank()) {
            String title = "DidYouDoIt? - " + task.getTitle();
            String body = customMsg.trim() + " (" + minutesOverdue + "m overdue)";
            return new NagMessage(title, body, level);
        }

        String headline = buildHeadline(task, activePersonality, level);
        String body = buildBody(task, activePersonality, level, minutesOverdue);

        return new NagMessage(headline, body, level);
    }

    private EscalationLevel calculateEscalationLevel(int minutesOverdue, int nagCount) {
        if (minutesOverdue > 45 || nagCount >= 3) {
            return EscalationLevel.CRITICAL;
        } else if (minutesOverdue > 15 || nagCount >= 2) {
            return EscalationLevel.WARN;
        } else if (minutesOverdue > 0 || nagCount >= 1) {
            return EscalationLevel.NUDGE;
        } else {
            return EscalationLevel.INITIAL;
        }
    }

    private String buildHeadline(Task task, PersonalityType personality, EscalationLevel level) {
        return switch (personality) {
            case GENTLE -> switch (level) {
                case INITIAL -> "🌸 Reminder: " + task.getTitle();
                case NUDGE -> "✨ Gentle Nudge: " + task.getTitle();
                case WARN -> "💖 Thinking of you: " + task.getTitle();
                case CRITICAL -> "🌟 Don't give up: " + task.getTitle();
            };
            case STRICT -> switch (level) {
                case INITIAL -> "📋 Due Now: " + task.getTitle();
                case NUDGE -> "⏱ Overdue: " + task.getTitle();
                case WARN -> "⚠️ Attention Required: " + task.getTitle();
                case CRITICAL -> "🚨 CRITICAL: " + task.getTitle();
            };
            case SARCASTIC -> switch (level) {
                case INITIAL -> "😏 Well, look who's due: " + task.getTitle();
                case NUDGE -> "👀 Still waiting: " + task.getTitle();
                case WARN -> "🏆 Procrastination Champion: " + task.getTitle();
                case CRITICAL -> "💀 RIP Productivity: " + task.getTitle();
            };
            case AGGRESSIVE -> switch (level) {
                case INITIAL -> "🔥 WAKE UP! " + task.getTitle();
                case NUDGE -> "⚡ MOVE IT! " + task.getTitle();
                case WARN -> "💥 NO EXCUSES! " + task.getTitle();
                case CRITICAL -> "🚨 DROP EVERYTHING AND DO IT! " + task.getTitle();
            };
        };
    }

    private String buildBody(Task task, PersonalityType personality, EscalationLevel level, int minutesOverdue) {
        String title = task.getTitle();
        return switch (personality) {
            case GENTLE -> switch (level) {
                case INITIAL -> "It's time for '" + title + "'. You've got this!";
                case NUDGE -> "'" + title + "' is waiting (" + minutesOverdue + "m ago). Take a small step right now.";
                case WARN -> "Just checking in on '" + title + "'. Finishing this will make you feel so much better!";
                case CRITICAL -> "It's been " + minutesOverdue + "m since '" + title + "' was due. Be kind to future you and do it!";
            };
            case STRICT -> switch (level) {
                case INITIAL -> "Task '" + title + "' is scheduled for now. Begin immediately.";
                case NUDGE -> "'" + title + "' is " + minutesOverdue + " minutes overdue. Stop delaying and execute.";
                case WARN -> "Discipline over motivation. Complete '" + title + "' without further delay.";
                case CRITICAL -> "Severely overdue by " + minutesOverdue + " minutes. Complete '" + title + "' now.";
            };
            case SARCASTIC -> switch (level) {
                case INITIAL -> "Are you actually going to do '" + title + "', or just admire this notification?";
                case NUDGE -> "'" + title + "' is " + minutesOverdue + "m late. Bold strategy. Let's see if magic finishes it.";
                case WARN -> "Still putting off '" + title + "'? Your future self is actively facepalming right now.";
                case CRITICAL -> "It's been " + minutesOverdue + "m. Did you think '" + title + "' was going to do itself? Do it now! 💀";
            };
            case AGGRESSIVE -> switch (level) {
                case INITIAL -> "PUT DOWN THE DISTRACTIONS! '" + title + "' IS DUE RIGHT NOW!";
                case NUDGE -> "" + minutesOverdue + " MINUTES WASTED! What happened to your discipline?! FINISH '" + title + "'!";
                case WARN -> "ARE YOU A QUITTER?! Finish '" + title + "' right this second or face the guilt!";
                case CRITICAL -> "DROP EVERYTHING! " + minutesOverdue + " MINUTES LATE! DO '" + title + "' OR ADMIT DEFEAT! 🚨";
            };
        };
    }
}
