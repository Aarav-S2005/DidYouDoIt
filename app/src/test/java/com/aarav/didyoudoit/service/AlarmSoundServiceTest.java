package com.aarav.didyoudoit.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AlarmSoundServiceTest {

    @Test
    @DisplayName("Play timer finished alarm does not throw exception even in headless/test environments")
    void testPlayTimerFinishedAlarm() {
        AlarmSoundService service = new AlarmSoundServiceImpl();
        assertDoesNotThrow(service::playTimerFinishedAlarm);
    }
}
