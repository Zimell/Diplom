package org.example.volonterhoursapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import org.example.volonterhoursapp.config.AppConfig;
import org.example.volonterhoursapp.db.DatabaseManager;
import org.example.volonterhoursapp.model.Role;
import org.example.volonterhoursapp.model.User;
import org.example.volonterhoursapp.ui.CuratorSession;
import org.example.volonterhoursapp.ui.ThemeManager;

import java.util.function.Consumer;

public class SettingsController implements Refreshable, StatusAware {

    @FXML private Label accountLabel;
    @FXML private Label roleLabel;
    @FXML private ToggleButton darkToggle;
    @FXML private ToggleButton lightToggle;
    @FXML private CheckBox accessibleCheck;
    @FXML private VBox dbCard;
    @FXML private Label dbInfoLabel;

    private Consumer<String> status = s -> {};

    @FXML
    private void initialize() {
        refresh();
    }

    @Override public void setStatusConsumer(Consumer<String> status) { this.status = status; }

    @Override
    public void refresh() {
        syncTheme();
        accessibleCheck.setSelected(ThemeManager.isAccessible());
        updateAccount();
        updateDbCard();
    }

    private void updateAccount() {
        User u = CuratorSession.getUser();
        Role role = CuratorSession.getRole();
        accountLabel.setText(u == null
                ? "—"
                : u.getFullName() + "  ·  логин: " + u.getUsername());
        roleLabel.setText(role == null ? "" : role.label);
    }

    private void updateDbCard() {
        boolean admin = CuratorSession.isAdmin();
        dbCard.setVisible(admin);
        dbCard.setManaged(admin);
        if (admin) {
            AppConfig cfg = DatabaseManager.config();
            if (cfg != null) {
                dbInfoLabel.setText("Сервер: " + cfg.getHost() + ":" + cfg.getPort()
                        + "\nБаза: " + cfg.getDatabase()
                        + "\nПользователь БД: " + cfg.getUsername());
            }
        }
    }

    private void syncTheme() {
        if (ThemeManager.getCurrent() == ThemeManager.Theme.DARK) darkToggle.setSelected(true);
        else lightToggle.setSelected(true);
    }

    @FXML private void onTheme() {
        if (!darkToggle.isSelected() && !lightToggle.isSelected()) {
            syncTheme();
            return;
        }
        if (lightToggle.isSelected() && ThemeManager.getCurrent() != ThemeManager.Theme.LIGHT) {
            ThemeManager.setTheme(ThemeManager.Theme.LIGHT);
        } else if (darkToggle.isSelected() && ThemeManager.getCurrent() != ThemeManager.Theme.DARK) {
            ThemeManager.setTheme(ThemeManager.Theme.DARK);
        }
        status.accept("Тема: "
                + (ThemeManager.getCurrent() == ThemeManager.Theme.DARK ? "тёмная" : "светлая")
                + (ThemeManager.isAccessible() ? " + для слабовидящих" : ""));
    }

    @FXML private void onAccessible() {
        ThemeManager.setAccessible(accessibleCheck.isSelected());
        status.accept(accessibleCheck.isSelected()
                ? "Включена версия для слабовидящих."
                : "Версия для слабовидящих выключена.");
    }
}
