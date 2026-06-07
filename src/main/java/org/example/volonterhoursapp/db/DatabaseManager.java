package org.example.volonterhoursapp.db;

import org.example.volonterhoursapp.config.AppConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public final class DatabaseManager {

    private static AppConfig config;

    private DatabaseManager() {}

    public static void initialize(AppConfig cfg) throws SQLException, IOException {
        config = cfg;
        ensureDatabaseExists();
        runSchemaScript();
    }

    public static Connection getConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", config.getUsername());
        props.setProperty("password", config.getPassword());
        return DriverManager.getConnection(config.jdbcUrlForDatabase(), props);
    }

    public static AppConfig config() {
        return config;
    }

    private static void ensureDatabaseExists() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", config.getUsername());
        props.setProperty("password", config.getPassword());

        try (Connection conn = DriverManager.getConnection(config.jdbcUrlForServer(), props)) {
            boolean exists;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM pg_database WHERE datname = ?")) {
                ps.setString(1, config.getDatabase());
                try (ResultSet rs = ps.executeQuery()) {
                    exists = rs.next();
                }
            }
            if (!exists) {
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate("CREATE DATABASE \"" + config.getDatabase().replace("\"", "\"\"") + "\"");
                }
            }
        }
    }

    private static void runSchemaScript() throws SQLException, IOException {
        String script = loadResource("/org/example/volonterhoursapp/schema.sql");
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {
            for (String statement : splitStatements(script)) {
                String trimmed = statement.trim();
                if (trimmed.isEmpty()) continue;
                st.execute(trimmed);
            }
        }
    }

    private static String loadResource(String path) throws IOException {
        try (InputStream in = DatabaseManager.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("Resource not found: " + path);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            return sb.toString();
        }
    }

    private static String[] splitStatements(String script) {
        StringBuilder sb = new StringBuilder();
        for (String line : script.split("\n")) {
            String s = line;
            int idx = s.indexOf("--");
            if (idx >= 0) s = s.substring(0, idx);
            sb.append(s).append('\n');
        }
        return sb.toString().split(";");
    }
}
