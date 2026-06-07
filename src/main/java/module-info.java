module org.example.volonterhoursapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;

    opens org.example.volonterhoursapp to javafx.fxml;
    opens org.example.volonterhoursapp.controller to javafx.fxml;
    opens org.example.volonterhoursapp.model to javafx.base;

    exports org.example.volonterhoursapp;
    exports org.example.volonterhoursapp.controller;
    exports org.example.volonterhoursapp.model;
    exports org.example.volonterhoursapp.ui;
}
