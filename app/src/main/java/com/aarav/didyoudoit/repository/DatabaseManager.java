package com.aarav.didyoudoit.repository;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface managing database connectivity and schema lifecycle.
 * Programmed to interface as required by architectural guidelines.
 */
public interface DatabaseManager extends AutoCloseable {

    /**
     * Obtains an active connection to the database.
     *
     * @return active {@link Connection}
     * @throws SQLException if a database access error occurs
     */
    Connection getConnection() throws SQLException;

    /**
     * Initializes database tables, indexes, and pragmas.
     */
    void initializeSchema();

    /**
     * Gets the file or URI path of the database.
     *
     * @return database location string
     */
    String getDatabasePath();

    @Override
    void close();
}
