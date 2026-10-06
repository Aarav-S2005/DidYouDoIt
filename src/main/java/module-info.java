module com.aarav.didyoudoit {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.aarav.didyoudoit to javafx.fxml;
    exports com.aarav.didyoudoit;
}