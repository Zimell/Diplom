package org.example.volonterhoursapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.example.volonterhoursapp.config.AppConfig;
import org.example.volonterhoursapp.controller.DbConfigController;
import org.example.volonterhoursapp.controller.LoginController;
import org.example.volonterhoursapp.controller.MainController;
import org.example.volonterhoursapp.db.DatabaseManager;
import org.example.volonterhoursapp.ui.CuratorSession;
import org.example.volonterhoursapp.ui.ThemeManager;

import java.io.IOException;
import java.sql.SQLException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) {
        AppConfig cfg;
        try {
            cfg = AppConfig.loadOrCreate();
        } catch (IOException e) {
            fatal("Не удалось создать/прочитать " + AppConfig.CONFIG_FILE_NAME, e);
            return;
        }
        ThemeManager.setTheme(ThemeManager.Theme.fromId(cfg.getTheme()));
        ThemeManager.setAccessible(cfg.isAccessibility());

        try {
            DatabaseManager.initialize(cfg);
        } catch (SQLException | IOException e) {
            openConfigDialog(stage, cfg, e.getMessage());
            return;
        }

        openLogin(stage);
    }

    public static void openLogin(Stage stage) {
        CuratorSession.logout();
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("views/login-view.fxml"));
            Parent root = loader.load();
            LoginController controller = loader.getController();
            Scene scene = new Scene(root, 560, 520);
            ThemeManager.register(scene);
            controller.init(stage);
            stage.setTitle("Учёт волонтёрских часов — вход");
            stage.setMinWidth(460);
            stage.setMinHeight(460);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            fatal("Ошибка загрузки окна входа", e);
        }
    }

    public static void openMain(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("views/main-view.fxml"));
            Parent root = loader.load();
            MainController controller = loader.getController();
            Scene scene = new Scene(root, 1200, 760);
            ThemeManager.register(scene);
            controller.attachStage(stage);
            stage.setTitle("Учёт волонтёрских часов");
            stage.setMinWidth(980);
            stage.setMinHeight(640);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            fatal("Ошибка загрузки главного окна", e);
        }
    }

    public static void openConfigDialog(Stage stage, AppConfig cfg, String error) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("views/db-config-view.fxml"));
            Parent root = loader.load();
            DbConfigController controller = loader.getController();
            controller.init(stage, cfg, error);
            Scene scene = new Scene(root, 640, 580);
            ThemeManager.register(scene);
            stage.setTitle("Учёт волонтёрских часов — настройка подключения");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            fatal("Ошибка диалога настроек", e);
        }
    }

    private static void fatal(String msg, Throwable t) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg + "\n\n" + (t == null ? "" : t.getMessage()));
        a.setHeaderText("Критическая ошибка");
        a.showAndWait();
        Platform.exit();
    }
}
