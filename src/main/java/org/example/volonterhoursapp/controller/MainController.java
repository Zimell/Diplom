package org.example.volonterhoursapp.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.volonterhoursapp.HelloApplication;
import org.example.volonterhoursapp.model.Role;
import org.example.volonterhoursapp.model.User;
import org.example.volonterhoursapp.ui.CuratorSession;
import org.example.volonterhoursapp.ui.ThemeManager;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Label sectionTitle;
    @FXML private Label statusLabel;
    @FXML private Label userLabel;
    @FXML private Label roleBadge;
    @FXML private Button logoutButton;
    @FXML private Button themeButton;
    @FXML private Button helpButton;

    @FXML private Button navDashboard;
    @FXML private Button navVolunteers;
    @FXML private Button navEvents;
    @FXML private Button navParticipations;
    @FXML private Button navInfo;
    @FXML private Button navAnalytics;
    @FXML private Button navRating;
    @FXML private Button navPlanner;
    @FXML private Button navAudit;
    @FXML private Button navUsers;
    @FXML private Button navSettings;

    /** Разделы, доступные обычному пользователю (роль USER). Остальные роли видят всё. */
    private static final Set<String> USER_SECTIONS =
            Set.of("dashboard", "events", "info", "rating", "settings");

    private final Map<String, ViewEntry> views = new LinkedHashMap<>();
    private Stage stage;
    private Stage helpStage;

    public void attachStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        applyRoleVisibility();
        updateAccountInfo();
        updateThemeButton();
        showDashboard();
    }

    private void updateAccountInfo() {
        User u = CuratorSession.getUser();
        Role role = CuratorSession.getRole();
        userLabel.setText(u == null ? "" : u.getFullName());
        roleBadge.setText(role == null ? "" : role.label);
    }

    /** Скрывает из меню разделы, недоступные текущей роли. */
    private void applyRoleVisibility() {
        setNavVisible(navDashboard,      "dashboard");
        setNavVisible(navVolunteers,     "volunteers");
        setNavVisible(navEvents,         "events");
        setNavVisible(navParticipations, "participations");
        setNavVisible(navInfo,           "info");
        setNavVisible(navAnalytics,      "analytics");
        setNavVisible(navRating,         "rating");
        setNavVisible(navPlanner,        "planner");
        setNavVisible(navAudit,          "audit");
        setNavVisible(navUsers,          "users");
        setNavVisible(navSettings,       "settings");
    }

    private void setNavVisible(Button btn, String key) {
        boolean visible = isAllowed(key);
        btn.setVisible(visible);
        btn.setManaged(visible);
    }

    private boolean isAllowed(String key) {
        Role role = CuratorSession.getRole();
        if ("users".equals(key)) return role == Role.ADMIN;
        if (role == Role.ADMIN || role == Role.CURATOR) return true;
        return USER_SECTIONS.contains(key);
    }

    @FXML private void onLogout() {
        HelloApplication.openLogin(stage);
    }

    private void updateThemeButton() {
        themeButton.setText(ThemeManager.getCurrent() == ThemeManager.Theme.DARK
                ? "Светлая тема" : "Тёмная тема");
    }

    public void setStatus(String s) {
        statusLabel.setText(s);
    }

    @FXML private void onToggleTheme() {
        ThemeManager.toggle();
        updateThemeButton();
    }

    /** Открывает встроенную справку. Окно создаётся один раз и переиспользуется. */
    @FXML private void onShowHelp() {
        if (helpStage != null && helpStage.getScene() != null) {
            helpStage.show();
            helpStage.toFront();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/org/example/volonterhoursapp/views/help-view.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            ThemeManager.register(scene);

            helpStage = new Stage();
            if (stage != null) helpStage.initOwner(stage);
            helpStage.setTitle("Справка — Учёт волонтёрских часов");
            helpStage.setScene(scene);
            helpStage.setMinWidth(820);
            helpStage.setMinHeight(560);
            helpStage.show();
        } catch (IOException e) {
            setStatus("Не удалось открыть справку: " + e.getMessage());
        }
    }

    @FXML private void onShowDashboard()      { show("dashboard",     "Дашборд",                 navDashboard); }
    @FXML private void onShowVolunteers()     { show("volunteers",    "Волонтёры",               navVolunteers); }
    @FXML private void onShowEvents()         { show("events",        "Мероприятия",             navEvents); }
    @FXML private void onShowParticipations() { show("participations","Участия",                 navParticipations); }
    @FXML private void onShowInfo()           { show("info",          "Информация о волонтёрах", navInfo); }
    @FXML private void onShowAnalytics()      { show("analytics",     "Аналитика участия",       navAnalytics); }
    @FXML private void onShowRating()         { show("rating",        "Рейтинг волонтёров",      navRating); }
    @FXML private void onShowPlanner()        { show("planner",       "План мероприятий",        navPlanner); }
    @FXML private void onShowAudit()          { show("audit",         "Журнал действий",         navAudit); }
    @FXML private void onShowUsers()          { show("users",         "Пользователи",            navUsers); }
    @FXML private void onShowSettings()       { show("settings",      "Настройки",               navSettings); }

    private void showDashboard() { onShowDashboard(); }

    private void show(String key, String title, Button btn) {
        ViewEntry entry = views.computeIfAbsent(key, this::loadView);
        if (entry == null) return;
        contentArea.getChildren().setAll(entry.node);
        sectionTitle.setText(title);
        highlight(btn);
        if (entry.controller instanceof Refreshable r) {
            r.refresh();
        }
    }

    private void highlight(Button activeBtn) {
        for (Button b : new Button[]{
                navDashboard, navVolunteers, navEvents, navParticipations,
                navInfo, navAnalytics, navRating, navPlanner,
                navAudit, navUsers, navSettings}) {
            b.getStyleClass().remove("active");
        }
        activeBtn.getStyleClass().add("active");
    }

    private ViewEntry loadView(String key) {
        String fxml = "/org/example/volonterhoursapp/views/" + key + "-view.fxml";
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Node n = loader.load();
            Object c = loader.getController();
            if (c instanceof StatusAware sa) sa.setStatusConsumer(this::setStatus);
            return new ViewEntry(n, c);
        } catch (IOException e) {
            setStatus("Не удалось загрузить раздел «" + key + "»: " + e.getMessage());
            return null;
        }
    }

    private record ViewEntry(Node node, Object controller) {}
}
