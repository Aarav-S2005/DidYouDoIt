package com.aarav.didyoudoit.service;

import java.io.File;

/**
 * Service for exporting and importing complete application data (tasks, habits, settings)
 * to/from JSON files for backups and migrations.
 * Implements FR-25.
 */
public interface DataBackupService {

    /**
     * Exports all user tasks, templates, and application settings to a JSON file.
     *
     * @param targetFile the file destination
     */
    void exportBackup(File targetFile);

    /**
     * Restores application data from a JSON backup file.
     *
     * @param sourceFile the source JSON file
     */
    void importBackup(File sourceFile);
}
