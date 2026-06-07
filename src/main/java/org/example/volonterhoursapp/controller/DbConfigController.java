package org.example.volonterhoursapp.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.volonterhoursapp.HelloApplication;
import org.example.volonterhoursapp.config.AppConfig;
import org.example.volonterhoursapp.db.DatabaseManager;

public class DbConfigController {

    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField dbField;
    @FXML private TextField userField;
    @FXML private PasswordField passField;
    @FXML private TextArea errorArea;

    private Stage stage;
    private AppConfig config;

    public void init(Stage stage, AppConfig config, String initialError) {
        this.stage = stage;
        this.config = config;
        hostField.setText(config.getHost());
        portField.setText(config.getPort());
        dbField.setText(config.getDatabase());
        userField.setText(config.getUsername());
        passField.setText(config.getPassword());
        if (initialError != null && !initialError.isBlank()) {
            errorArea.setText(initialError);
            errorArea.setVisible(true);
            errorArea.setManaged(true);
        }
    }

    @FXML
    private void onCancel() {
        Platform.exit();
    }

    @FXML
    private void onSave() {
        config.setHost(hostField.getText().trim());
        config.setPort(portField.getText().trim());
        config.setDatabase(dbField.getText().trim());
        config.setUsername(userField.getText().trim());
        config.setPassword(passField.getText());
        try {
            config.save();
            DatabaseManager.initialize(config);
            HelloApplication.openMain(stage);
        } catch (Exception ex) {
            errorArea.setText(ex.getMessage());
            errorArea.setVisible(true);
            errorArea.setManaged(true);
        }
    }
}
