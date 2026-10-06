package com.aarav.didyoudoit.service;

/**
 * Service managing Windows user login auto-start registration.
 * Implements FR-20, NFR-05, NFR-11.
 */
public interface StartupService {

    /**
     * Checks if DidYouDoIt is registered to run on Windows user login.
     *
     * @return true if startup registry entry exists and is active
     */
    boolean isAutoStartEnabled();

    /**
     * Enables or disables launching the application on Windows user login.
     *
     * @param enabled true to register in Windows startup, false to unregister
     * @return true if the registry operation succeeded
     */
    boolean setAutoStartEnabled(boolean enabled);
}
