package com.aarav.didyoudoit.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Clock abstraction interface for querying the current time.
 * Allows time-dependent services (streaks, quiet hours, recurrence) to be unit tested deterministically.
 */
public interface ClockService {

    /**
     * Gets the current date and time.
     */
    LocalDateTime now();

    /**
     * Gets the current local date.
     */
    default LocalDate today() {
        return now().toLocalDate();
    }

    /**
     * Gets the current local time of day.
     */
    default LocalTime currentTime() {
        return now().toLocalTime();
    }
}
