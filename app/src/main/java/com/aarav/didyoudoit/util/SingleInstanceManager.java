package com.aarav.didyoudoit.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ensures only a single instance of DidYouDoIt runs per user session.
 * If a second instance is launched, it notifies the first instance to bring its window to front,
 * then terminates immediately, preventing duplicate background runners or duplicate tray icons.
 * Implements NFR-01, NFR-02, NFR-15.
 */
public final class SingleInstanceManager {

    private static final Logger LOGGER = Logger.getLogger(SingleInstanceManager.class.getName());
    private static final int LOCK_PORT = 47289;
    private static ServerSocket serverSocket;

    private static volatile Runnable globalActivateCallback;

    /**
     * Checks if another instance is already running before JavaFX boots.
     * If already running, sends ACTIVATE signal and returns false.
     */
    public static boolean checkAndAcquireLockEarly() {
        return acquireInstanceLock(() -> {
            if (globalActivateCallback != null) {
                globalActivateCallback.run();
            }
        });
    }

    public static void setActivationListener(Runnable callback) {
        globalActivateCallback = callback;
    }

    /**
     * Checks if another instance is already running.
     * If running, sends an "ACTIVATE" message to the running instance and returns false.
     * If not running, starts a local listener on 127.0.0.1:47289 and returns true.
     *
     * @param onActivateRequested callback invoked on the first instance when a duplicate launch occurs
     * @return true if this is the sole/primary instance, false if another instance was already active
     */
    public static synchronized boolean acquireInstanceLock(Runnable onActivateRequested) {
        if (serverSocket != null && !serverSocket.isClosed()) {
            if (onActivateRequested != null) {
                globalActivateCallback = onActivateRequested;
            }
            return true;
        }

        try {
            serverSocket = new ServerSocket(LOCK_PORT, 10, InetAddress.getByName("127.0.0.1"));
            LOGGER.info("Successfully acquired single instance lock on port " + LOCK_PORT);

            if (onActivateRequested != null) {
                globalActivateCallback = onActivateRequested;
            }

            Thread listenerThread = new Thread(() -> {
                while (serverSocket != null && !serverSocket.isClosed()) {
                    try (Socket clientSocket = serverSocket.accept();
                         BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                        String line = in.readLine();
                        if ("ACTIVATE".equalsIgnoreCase(line) && globalActivateCallback != null) {
                            LOGGER.info("Duplicate instance launch detected. Activating primary window.");
                            globalActivateCallback.run();
                        }
                    } catch (Exception ignored) {}
                }
            }, "didyoudoit-single-instance-listener");
            listenerThread.setDaemon(true);
            listenerThread.start();

            return true;
        } catch (Exception e) {
            // Port is already occupied: notify the running instance
            LOGGER.info("Another instance of DidYouDoIt is already running. Notifying primary instance...");
            try (Socket socket = new Socket(InetAddress.getByName("127.0.0.1"), LOCK_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                out.println("ACTIVATE");
            } catch (Exception notifyEx) {
                LOGGER.log(Level.FINE, "Failed to send activate signal to primary instance: " + notifyEx.getMessage());
            }
            return false;
        }
    }

    public static void releaseInstanceLock() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (Exception ignored) {}
        }
    }
}
