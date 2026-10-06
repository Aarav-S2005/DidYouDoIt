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

        // Gentle Initial
        PersonalityMessageService.NagMessage gentle = messageService.generateMessage(task, PersonalityType.GENTLE, 0);
        assertTrue(gentle.title().contains("🌸"));
        assertEquals(EscalationLevel.INITIAL, gentle.level());

        // Strict Nudge
        PersonalityMessageService.NagMessage strict = messageService.generateMessage(task, PersonalityType.STRICT, 10);
        assertTrue(strict.title().contains("Overdue"));
        assertEquals(EscalationLevel.NUDGE, strict.level());

        // Sarcastic Warn
        PersonalityMessageService.NagMessage sarcastic = messageService.generateMessage(task, PersonalityType.SARCASTIC, 25);
        assertTrue(sarcastic.title().contains("Procrastination"));
        assertEquals(EscalationLevel.WARN, sarcastic.level());

        // Aggressive Critical
        PersonalityMessageService.NagMessage aggressive = messageService.generateMessage(task, PersonalityType.AGGRESSIVE, 60);
        assertTrue(aggressive.title().contains("DROP EVERYTHING"));
        assertEquals(EscalationLevel.CRITICAL, aggressive.level());
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
