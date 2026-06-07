package org.example.volonterhoursapp.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public final class AppConfig {

    public static final String CONFIG_FILE_NAME = "db-config.txt";

    private String host;
    private String port;
    private String database;
    private String username;
    private String password;
    private String theme;
    private boolean accessibility;

    public AppConfig() {
        this.host = "localhost";
        this.port = "5432";
        this.database = "volunteer_hours";
        this.username = "postgres";
        this.password = "postgres";
        this.theme = "dark";
        this.accessibility = false;
    }

    public static Path configPath() {
        return Paths.get(CONFIG_FILE_NAME).toAbsolutePath();
    }

    public static AppConfig loadOrCreate() throws IOException {
        AppConfig cfg = new AppConfig();
        Path path = configPath();
        if (!Files.exists(path)) {
            cfg.save();
            return cfg;
        }
        Properties props = new Properties();
        try (var in = Files.newBufferedReader(path)) {
            props.load(in);
        }
        cfg.host     = props.getProperty("host", cfg.host).trim();
        cfg.port     = props.getProperty("port", cfg.port).trim();
        cfg.database = props.getProperty("dbname", props.getProperty("database", cfg.database)).trim();
        cfg.username = props.getProperty("username", cfg.username).trim();
        cfg.password = props.getProperty("password", cfg.password);
        cfg.theme    = props.getProperty("theme", cfg.theme).trim().toLowerCase();
        cfg.accessibility = Boolean.parseBoolean(
                props.getProperty("accessibility", String.valueOf(cfg.accessibility)).trim());
        return cfg;
    }

    public void save() throws IOException {
        Map<String, String> kv = new LinkedHashMap<>();
        kv.put("host", host);
        kv.put("port", port);
        kv.put("dbname", database);
        kv.put("username", username);
        kv.put("password", password);
        kv.put("theme", theme);
        kv.put("accessibility", String.valueOf(accessibility));

        StringBuilder sb = new StringBuilder();
        sb.append("# Volunteer Hours - PostgreSQL connection configuration\n");
        sb.append("# Edit these values and restart the application.\n");
        sb.append("# theme: dark | light\n");
        sb.append("# accessibility: true | false (версия для слабовидящих)\n\n");
        for (Map.Entry<String, String> e : kv.entrySet()) {
            sb.append(e.getKey()).append('=').append(e.getValue() == null ? "" : e.getValue()).append('\n');
        }
        Files.writeString(configPath(), sb.toString());
    }

    public String jdbcUrlForServer() {
        return "jdbc:postgresql://" + host + ":" + port + "/postgres";
    }

    public String jdbcUrlForDatabase() {
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }

    public String getHost()     { return host; }
    public String getPort()     { return port; }
    public String getDatabase() { return database; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getTheme()    { return theme; }
    public boolean isAccessibility() { return accessibility; }

    public void setHost(String v)     { this.host = v; }
    public void setPort(String v)     { this.port = v; }
    public void setDatabase(String v) { this.database = v; }
    public void setUsername(String v) { this.username = v; }
    public void setPassword(String v) { this.password = v; }
    public void setTheme(String v)    { this.theme = v; }
    public void setAccessibility(boolean v) { this.accessibility = v; }
}
