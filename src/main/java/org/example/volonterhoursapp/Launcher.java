package org.example.volonterhoursapp;

import javafx.application.Application;

/**
 * Точка входа для упакованного приложения (.exe). Не наследует {@link Application},
 * поэтому JavaFX корректно запускается, когда модули лежат на classpath, а не на module-path.
 */
public final class Launcher {
    public static void main(String[] args) {
        Application.launch(HelloApplication.class, args);
    }
}
