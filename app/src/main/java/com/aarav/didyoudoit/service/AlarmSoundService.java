package com.aarav.didyoudoit.service;

/**
 * Service for playing audible completion alarms and notification sounds.
 * Implements audio feedback for focus timer sessions without blocking UI threads.
 */
public interface AlarmSoundService {

    /**
     * Asynchronously plays a melodious completion alarm chime when a focus session finishes.
     */
    void playTimerFinishedAlarm();
}
