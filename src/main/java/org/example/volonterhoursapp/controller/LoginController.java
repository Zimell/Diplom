package org.example.volonterhoursapp.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.volonterhoursapp.HelloApplication;
import org.example.volonterhoursapp.dao.UserDAO;
import org.example.volonterhoursapp.model.User;
import org.example.volonterhoursapp.ui.CuratorSession;

import java.sql.SQLException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    private final UserDAO userDAO = new UserDAO();
    private Stage stage;

    public void init(Stage stage) {
        this.stage = stage;
        Platform.runLater(() -> usernameField.requestFocus());
    }

    @FXML
    private void initialize() {
        // Enter в любом из полей запускает вход.
        usernameField.setOnAction(e -> onLogin());
        passwordField.setOnAction(e -> onLogin());
    }

    @FXML
    private void onLogin() {
        hideError();
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Введите логин и пароль.");
            return;
        }

        loginButton.setDisable(true);
        try {
            User user = userDAO.authenticate(username, password);
            if (user == null) {
                showError("Неверный логин или пароль.");
                passwordField.clear();
                passwordField.requestFocus();
                return;
            }
            CuratorSession.login(user);
            HelloApplication.openMain(stage);
        } catch (SQLException e) {
            showError("Ошибка подключения к базе данных: " + e.getMessage());
        } finally {
            loginButton.setDisable(false);
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
