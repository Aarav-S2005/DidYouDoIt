module com.aarav.didyoudoit {
    requires javafx.controls;
    requires javafx.graphics;
    requires java.sql;
    requires com.sun.jna;
    requires com.sun.jna.platform;

    exports com.aarav.didyoudoit;
    exports com.aarav.didyoudoit.model;
    exports com.aarav.didyoudoit.repository;
    exports com.aarav.didyoudoit.service;
    exports com.aarav.didyoudoit.ui.theme;
    exports com.aarav.didyoudoit.util;
}