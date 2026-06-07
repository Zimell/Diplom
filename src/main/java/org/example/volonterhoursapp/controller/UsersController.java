package org.example.volonterhoursapp.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.example.volonterhoursapp.dao.AuditDAO;
import org.example.volonterhoursapp.dao.UserDAO;
import org.example.volonterhoursapp.model.Role;
import org.example.volonterhoursapp.model.User;
import org.example.volonterhoursapp.ui.CuratorSession;
import org.example.volonterhoursapp.ui.UiUtils;

import java.sql.SQLException;
import java.util.function.Consumer;

public class UsersController implements Refreshable, StatusAware {

    @FXML private TableView<User> table;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colFullName;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colActive;

    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private ComboBox<Role> roleBox;
    @FXML private CheckBox activeCheck;
    @FXML private PasswordField passwordField;

    private final UserDAO dao = new UserDAO();
    private final AuditDAO audit = new AuditDAO();
    private final ObservableList<User> items = FXCollections.observableArrayList();
    private Consumer<String> status = s -> {};

    @FXML
    private void initialize() {
        colUsername.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        colFullName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));
        colRole.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole().label));
        colActive.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isActive() ? "да" : "нет"));

        roleBox.setItems(FXCollections.observableArrayList(Role.values()));
        roleBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Role r) { return r == null ? "" : r.label; }
            @Override public Role fromString(String s) { return null; }
        });

        table.setItems(items);
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> populateForm(b));
    }

    @Override public void setStatusConsumer(Consumer<String> status) { this.status = status; }

    @Override
    public void refresh() {
        try {
            items.setAll(dao.findAll());
            status.accept("Учётных записей: " + items.size());
        } catch (SQLException ex) {
            status.accept("Ошибка: " + ex.getMessage());
        }
    }

    @FXML private void onNew() {
        table.getSelectionModel().clearSelection();
        usernameField.clear();
        fullNameField.clear();
        roleBox.setValue(Role.USER);
        activeCheck.setSelected(true);
        passwordField.clear();
        passwordField.setPromptText("Пароль (обязателен)");
        usernameField.requestFocus();
    }

    @FXML private void onSave() {
        String username = text(usernameField);
        String fullName = text(fullNameField);
        Role role = roleBox.getValue();
        boolean active = activeCheck.isSelected();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (username.isEmpty()) { UiUtils.error("Логин обязателен."); return; }
        if (fullName.isEmpty()) { UiUtils.error("ФИО обязательно."); return; }
        if (role == null) { UiUtils.error("Выберите роль."); return; }

        User current = table.getSelectionModel().getSelectedItem();
        try {
            if (dao.usernameExists(username, current == null ? null : current.getId())) {
                UiUtils.error("Логин «" + username + "» уже занят.");
                return;
            }

            if (current == null) {
                if (password.isEmpty()) { UiUtils.error("Для нового пользователя нужно задать пароль."); return; }
                User u = new User();
                u.setUsername(username);
                u.setFullName(fullName);
                u.setRole(role);
                u.setActive(active);
                dao.insert(u, password);
                audit.log(CuratorSession.getUserId(), "user.create", username + " (" + role.label + ")");
                status.accept("Создан пользователь «" + username + "»");
            } else {
                if (wouldRemoveLastAdmin(current, role, active)) {
                    UiUtils.error("Нельзя убрать последнего активного администратора.");
                    return;
                }
                current.setUsername(username);
                current.setFullName(fullName);
                current.setRole(role);
                current.setActive(active);
                dao.update(current);
                if (!password.isEmpty()) dao.updatePassword(current.getId(), password);
                audit.log(CuratorSession.getUserId(), "user.update",
                        username + (password.isEmpty() ? "" : " (пароль изменён)"));
                status.accept("Сохранены изменения для «" + username + "»");
            }
            refresh();
        } catch (SQLException ex) {
            UiUtils.error("Не удалось сохранить: " + ex.getMessage());
        }
    }

    @FXML private void onDelete() {
        User u = table.getSelectionModel().getSelectedItem();
        if (u == null) { status.accept("Выберите пользователя."); return; }

        User me = CuratorSession.getUser();
        if (me != null && me.getId() == u.getId()) {
            UiUtils.error("Нельзя удалить собственную учётную запись.");
            return;
        }
        try {
            if (wouldRemoveLastAdmin(u, null, false)) {
                UiUtils.error("Нельзя удалить последнего активного администратора.");
                return;
            }
            if (!UiUtils.confirm("Удалить пользователя «" + u.getUsername() + "»?")) return;
            dao.delete(u.getId());
            audit.log(CuratorSession.getUserId(), "user.delete", u.getUsername());
            refresh();
            onNew();
        } catch (SQLException ex) {
            UiUtils.error("Не удалось удалить: " + ex.getMessage());
        }
    }

    /** true, если изменение/удаление {@code target} оставит систему без активных админов. */
    private boolean wouldRemoveLastAdmin(User target, Role newRole, boolean newActive) throws SQLException {
        boolean wasActiveAdmin = target.getRole() == Role.ADMIN && target.isActive();
        if (!wasActiveAdmin) return false;
        boolean staysActiveAdmin = newRole == Role.ADMIN && newActive;
        if (staysActiveAdmin) return false;
        return dao.countActiveAdmins() <= 1;
    }

    private void populateForm(User u) {
        if (u == null) return;
        usernameField.setText(u.getUsername());
        fullNameField.setText(u.getFullName());
        roleBox.setValue(u.getRole());
        activeCheck.setSelected(u.isActive());
        passwordField.clear();
        passwordField.setPromptText("Оставьте пустым, чтобы не менять");
    }

    private static String text(TextField f) {
        String s = f.getText();
        return s == null ? "" : s.trim();
    }
}
