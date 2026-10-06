package com.aarav.didyoudoit.util;

import java.time.LocalDateTime;

/**
 * Mutable clock implementation for unit testing time travel and schedule triggers.
 */
public class TestClockService implements ClockService {

    private LocalDateTime currentTime;

    public TestClockService(LocalDateTime initialTime) {
        this.currentTime = initialTime;
    }

    @Override
    public LocalDateTime now() {
        return currentTime;
    }

    public void setTime(LocalDateTime time) {
        this.currentTime = time;
    }

    public void advanceHours(long hours) {
        this.currentTime = this.currentTime.plusHours(hours);
    }

    public void advanceDays(long days) {
        this.currentTime = this.currentTime.plusDays(days);
    }

    public void advanceMinutes(long minutes) {
        this.currentTime = this.currentTime.plusMinutes(minutes);
    }
}
