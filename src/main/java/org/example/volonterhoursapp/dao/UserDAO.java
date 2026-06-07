package org.example.volonterhoursapp.dao;

import org.example.volonterhoursapp.db.DatabaseManager;
import org.example.volonterhoursapp.model.Role;
import org.example.volonterhoursapp.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public class UserDAO {

    /**
     * Проверяет логин и пароль. Возвращает пользователя при успешном входе,
     * либо {@code null}, если учётной записи нет, она отключена или пароль неверный.
     */
    public User authenticate(String username, String password) throws SQLException {
        if (username == null || username.isBlank() || password == null) return null;

        String sql = "SELECT id, username, password_hash, full_name, role, active "
                + "FROM users WHERE username = ? AND active = TRUE";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String storedHash = rs.getString("password_hash");
                if (!sha256(password).equalsIgnoreCase(storedHash)) return null;
                return map(rs);
            }
        }
    }

    public List<User> findAll() throws SQLException {
        String sql = "SELECT id, username, password_hash, full_name, role, active "
                + "FROM users ORDER BY active DESC, role, username";
        List<User> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /** Проверяет, занят ли логин кем-то, кроме пользователя с {@code excludeId} (может быть null). */
    public boolean usernameExists(String username, Long excludeId) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE LOWER(username) = LOWER(?) AND (? IS NULL OR id <> ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            if (excludeId == null) {
                ps.setNull(2, java.sql.Types.BIGINT);
                ps.setNull(3, java.sql.Types.BIGINT);
            } else {
                ps.setLong(2, excludeId);
                ps.setLong(3, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long insert(User u, String plainPassword) throws SQLException {
        String sql = "INSERT INTO users(username, password_hash, full_name, role, active) "
                + "VALUES (?,?,?,?,?) RETURNING id";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getUsername().trim());
            ps.setString(2, sha256(plainPassword));
            ps.setString(3, u.getFullName());
            ps.setString(4, u.getRole().id);
            ps.setBoolean(5, u.isActive());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                long id = rs.getLong(1);
                u.setId(id);
                return id;
            }
        }
    }

    /** Обновляет профиль (без пароля). */
    public void update(User u) throws SQLException {
        String sql = "UPDATE users SET username=?, full_name=?, role=?, active=? WHERE id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getUsername().trim());
            ps.setString(2, u.getFullName());
            ps.setString(3, u.getRole().id);
            ps.setBoolean(4, u.isActive());
            ps.setLong(5, u.getId());
            ps.executeUpdate();
        }
    }

    public void updatePassword(long id, String plainPassword) throws SQLException {
        String sql = "UPDATE users SET password_hash=? WHERE id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sha256(plainPassword));
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /** Сколько в системе активных администраторов — чтобы не остаться без единственного админа. */
    public int countActiveAdmins() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role='admin' AND active=TRUE";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setFullName(rs.getString("full_name"));
        u.setRole(Role.fromId(rs.getString("role")));
        u.setActive(rs.getBoolean("active"));
        return u;
    }

    /** SHA-256(пароль) в нижнем регистре (hex) — тот же формат, что в schema.sql. */
    public static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
