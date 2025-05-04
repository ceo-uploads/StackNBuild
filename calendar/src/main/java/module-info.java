module org.openjfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens org.openjfx to javafx.fxml;
    exports org.openjfx;
}
