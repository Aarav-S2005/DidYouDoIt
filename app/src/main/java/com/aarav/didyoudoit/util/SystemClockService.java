package com.aarav.didyoudoit.util;

import java.time.LocalDateTime;

/**
 * Default system clock implementation using system local time.
 */
public class SystemClockService implements ClockService {

    @Override
    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}
