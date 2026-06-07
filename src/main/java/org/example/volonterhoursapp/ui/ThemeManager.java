package org.example.volonterhoursapp.ui;

import javafx.scene.Scene;
import org.example.volonterhoursapp.config.AppConfig;
import org.example.volonterhoursapp.db.DatabaseManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ThemeManager {

    public enum Theme {
        DARK("dark", "/org/example/volonterhoursapp/styles/dark.css"),
        LIGHT("light", "/org/example/volonterhoursapp/styles/light.css");

        public final String id;
        public final String css;

        Theme(String id, String css) { this.id = id; this.css = css; }

        public static Theme fromId(String id) {
            if ("light".equalsIgnoreCase(id)) return LIGHT;
            return DARK;
        }
    }

    private static final String COMMON_CSS = "/org/example/volonterhoursapp/styles/common.css";
    /** Версия «для слабовидящих»: крупный шрифт и повышенная контрастность поверх любой темы. */
    private static final String A11Y_CSS = "/org/example/volonterhoursapp/styles/a11y.css";

    private static Theme current = Theme.DARK;
    private static boolean accessible = false;
    private static final List<Scene> registered = new ArrayList<>();

    private ThemeManager() {}

    public static Theme getCurrent() { return current; }

    public static boolean isAccessible() { return accessible; }

    public static void register(Scene scene) {
        if (!registered.contains(scene)) registered.add(scene);
        apply(scene);
    }

    public static void setTheme(Theme t) {
        current = t;
        reapplyAll();
        persist();
    }

    /** Включает/выключает режим для слабовидящих («плюсик» к текущей теме). */
    public static void setAccessible(boolean on) {
        accessible = on;
        reapplyAll();
        persist();
    }

    public static void toggle() {
        setTheme(current == Theme.DARK ? Theme.LIGHT : Theme.DARK);
    }

    private static void reapplyAll() {
        for (Scene s : registered) apply(s);
    }

    private static void persist() {
        AppConfig cfg = DatabaseManager.config();
        if (cfg != null) {
            cfg.setTheme(current.id);
            cfg.setAccessibility(accessible);
            try { cfg.save(); } catch (IOException ignored) {}
        }
    }

    private static void apply(Scene scene) {
        scene.getStylesheets().clear();
        scene.getStylesheets().add(url(COMMON_CSS));
        scene.getStylesheets().add(url(current.css));
        if (accessible) scene.getStylesheets().add(url(A11Y_CSS));
    }

    private static String url(String resource) {
        return Objects.requireNonNull(ThemeManager.class.getResource(resource)).toExternalForm();
    }
}
