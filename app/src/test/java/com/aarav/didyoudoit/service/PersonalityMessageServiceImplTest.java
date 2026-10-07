package com.aarav.didyoudoit.service;

import com.aarav.didyoudoit.model.EscalationLevel;
import com.aarav.didyoudoit.model.PersonalityType;
import com.aarav.didyoudoit.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PersonalityMessageServiceImplTest {

    private PersonalityMessageService messageService;

    @BeforeEach
    void setUp() {
        messageService = new PersonalityMessageServiceImpl();
    }

    @Test
    @DisplayName("Generates escalating messages across personalities")
    void testMessageGenerationAcrossPersonalities() {
        Task task = Task.builder().title("File Tax Return").build();

        // Gentle Initial (nagCount = 0)
        task.setNagCount(0);
        PersonalityMessageService.NagMessage gentle = messageService.generateMessage(task, PersonalityType.GENTLE, 0);
        assertTrue(gentle.title().contains("File Tax Return"));
        assertFalse(gentle.body().isBlank());
        assertEquals(EscalationLevel.INITIAL, gentle.level());

        // Strict Nudge (nagCount = 2)
        task.setNagCount(2);
        PersonalityMessageService.NagMessage strict = messageService.generateMessage(task, PersonalityType.STRICT, 10);
        assertTrue(strict.title().contains("File Tax Return"));
        assertFalse(strict.body().isBlank());
        assertEquals(EscalationLevel.NUDGE, strict.level());

        // Sarcastic Warn (nagCount = 4)
        task.setNagCount(4);
        PersonalityMessageService.NagMessage sarcastic = messageService.generateMessage(task, PersonalityType.SARCASTIC, 25);
        assertTrue(sarcastic.title().contains("File Tax Return"));
        assertFalse(sarcastic.body().isBlank());
        assertEquals(EscalationLevel.WARN, sarcastic.level());

        // Aggressive Critical (nagCount = 7)
        task.setNagCount(7);
        PersonalityMessageService.NagMessage aggressive = messageService.generateMessage(task, PersonalityType.AGGRESSIVE, 60);
        assertTrue(aggressive.title().contains("File Tax Return"));
        assertFalse(aggressive.body().isBlank());
        assertEquals(EscalationLevel.CRITICAL, aggressive.level());
    }

    @Test
    @DisplayName("Randomized message pools generate varied content across repeated calls")
    void testRandomizedPoolDiversity() {
        Task task = Task.builder().title("Gym Workout").build();
        java.util.Set<String> bodies = new java.util.HashSet<>();

        for (int i = 0; i < 30; i++) {
            var msg = messageService.generateMessage(task, PersonalityType.SARCASTIC, 5);
            bodies.add(msg.body());
        }

        // Out of 30 samples from a pool of 10 items, we expect multiple distinct lines
        assertTrue(bodies.size() > 1, "Randomization should produce multiple distinct message variants");
    }

    @Test
    @DisplayName("Custom nag message is respected when provided")
    void testCustomNagMessage() {
        Task task = Task.builder()
                .title("Pay Rent")
                .customNagMessage("You will get evicted if you don't pay!")
                .build();

        PersonalityMessageService.NagMessage msg = messageService.generateMessage(task, PersonalityType.SARCASTIC, 15);
        assertTrue(msg.body().contains("You will get evicted if you don't pay!"));
    }
}
