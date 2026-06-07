package org.example.volonterhoursapp.dao;

import org.example.volonterhoursapp.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AuditDAO {

    /** {@code actor} — ФИО исполнителя (из JOIN users по actor_user_id), может быть null. */
    public record LogEntry(long id, Timestamp occurredAt, String actor, String action, String details) {}

    /** Базовый SELECT с присоединённым именем исполнителя. */
    private static final String SELECT_BASE = """
            SELECT a.id, a.occurred_at, u.full_name AS actor, a.action, a.details
              FROM audit_log a
              LEFT JOIN users u ON u.id = a.actor_user_id
            """;

    /** Записывает событие в журнал. {@code actorUserId} — id текущего пользователя (может быть null). */
    public void log(Long actorUserId, String action, String details) {
        String sql = "INSERT INTO audit_log(actor_user_id, action, details) VALUES (?,?,?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (actorUserId == null) ps.setNull(1, java.sql.Types.BIGINT);
            else ps.setLong(1, actorUserId);
            ps.setString(2, action);
            ps.setString(3, details);
            ps.executeUpdate();
        } catch (SQLException ignored) {
            // best-effort logging
        }
    }

    public List<LogEntry> search(String actor, String action, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append(" WHERE 1=1");
        if (actor != null && !actor.isBlank())   sql.append(" AND LOWER(COALESCE(u.full_name,'')) LIKE ?");
        if (action != null && !action.isBlank()) sql.append(" AND LOWER(COALESCE(a.action,''))    LIKE ?");
        sql.append(" ORDER BY a.occurred_at DESC LIMIT ?");
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int i = 1;
            if (actor != null && !actor.isBlank())   ps.setString(i++, "%" + actor.toLowerCase()  + "%");
            if (action != null && !action.isBlank()) ps.setString(i++, "%" + action.toLowerCase() + "%");
            ps.setInt(i, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<LogEntry> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    public List<LogEntry> findRecent(int limit) throws SQLException {
        return findRecent(limit, false);
    }

    /**
     * Последние записи журнала. Если {@code excludeAdmins} = true, скрываются действия,
     * совершённые администраторами — куратор видит только действия кураторов и волонтёров.
     */
    public List<LogEntry> findRecent(int limit, boolean excludeAdmins) throws SQLException {
        List<LogEntry> list = new ArrayList<>();
        String sql = SELECT_BASE
                + (excludeAdmins ? " WHERE " + EXCLUDE_ADMINS : "")
                + " ORDER BY a.occurred_at DESC LIMIT ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    private LogEntry map(ResultSet rs) throws SQLException {
        return new LogEntry(
                rs.getLong("id"),
                rs.getTimestamp("occurred_at"),
                rs.getString("actor"),
                rs.getString("action"),
                rs.getString("details"));
    }

    /** Исполнитель — не администратор (NULL-исполнители остаются видны). */
    private static final String EXCLUDE_ADMINS =
            "(a.actor_user_id IS NULL OR u.role <> 'admin')";
}
