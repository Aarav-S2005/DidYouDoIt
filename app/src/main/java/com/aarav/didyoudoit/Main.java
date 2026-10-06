package com.aarav.didyoudoit;

/**
 * Standard non-subclass entry point for packaging and fat JAR execution.
 * Circumvents the JavaFX runtime launcher check when started outside JavaFX module path.
 */
public final class Main {
    private Main() {}

    public static void main(String[] args) {
        DidYouDoItApp.main(args);
    }
}
