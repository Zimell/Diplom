package org.example.volonterhoursapp.dao;

import org.example.volonterhoursapp.db.DatabaseManager;
import org.example.volonterhoursapp.model.Participation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ParticipationDAO {

    private static final String SELECT_BASE = """
            SELECT p.id, p.volunteer_id, p.event_id, p.hours_worked,
                   p.confirmed, p.confirmed_by_user_id, p.confirmed_at,
                   v.fio AS volunteer_name, e.title AS event_title,
                   cu.full_name AS confirmed_by_name
              FROM participations p
              JOIN volunteers v ON v.id = p.volunteer_id
              JOIN events     e ON e.id = p.event_id
              LEFT JOIN users cu ON cu.id = p.confirmed_by_user_id
            """;

    public List<Participation> findAll() throws SQLException {
        List<Participation> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(SELECT_BASE + " ORDER BY p.id DESC")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public long insert(Participation p) throws SQLException {
        String sql = """
                INSERT INTO participations(volunteer_id, event_id, hours_worked, confirmed, confirmed_by_user_id, confirmed_at)
                VALUES (?,?,?,?,?,?) RETURNING id
                """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, p.getVolunteerId());
            ps.setLong(2, p.getEventId());
            ps.setBigDecimal(3, java.math.BigDecimal.valueOf(p.getHoursWorked()));
            ps.setBoolean(4, p.isConfirmed());
            setLongOrNull(ps, 5, p.getConfirmedByUserId());
            ps.setTimestamp(6, p.getConfirmedAt() == null ? null : Timestamp.valueOf(p.getConfirmedAt()));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                long id = rs.getLong(1);
                p.setId(id);
                return id;
            }
        }
    }

    public void update(Participation p) throws SQLException {
        String sql = """
                UPDATE participations
                   SET volunteer_id=?, event_id=?, hours_worked=?, confirmed=?, confirmed_by_user_id=?, confirmed_at=?
                 WHERE id=?
                """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, p.getVolunteerId());
            ps.setLong(2, p.getEventId());
            ps.setBigDecimal(3, java.math.BigDecimal.valueOf(p.getHoursWorked()));
            ps.setBoolean(4, p.isConfirmed());
            setLongOrNull(ps, 5, p.getConfirmedByUserId());
            ps.setTimestamp(6, p.getConfirmedAt() == null ? null : Timestamp.valueOf(p.getConfirmedAt()));
            ps.setLong(7, p.getId());
            ps.executeUpdate();
        }
    }

    public void confirm(long id, long confirmedByUserId) throws SQLException {
        String sql = "UPDATE participations SET confirmed=TRUE, confirmed_by_user_id=?, confirmed_at=? WHERE id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, confirmedByUserId);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    public void revoke(long id) throws SQLException {
        String sql = "UPDATE participations SET confirmed=FALSE, confirmed_by_user_id=NULL, confirmed_at=NULL WHERE id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM participations WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /** Sum of confirmed hours per volunteer within range (inclusive). */
    public double sumConfirmedHours(long volunteerId, LocalDate from, LocalDate to) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(p.hours_worked), 0) AS total
                  FROM participations p
                  JOIN events e ON e.id = p.event_id
                 WHERE p.volunteer_id = ?
                   AND p.confirmed = TRUE
                   AND e.datetime IS NOT NULL
                   AND e.datetime::date BETWEEN ? AND ?
                """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, volunteerId);
            ps.setDate(2, java.sql.Date.valueOf(from));
            ps.setDate(3, java.sql.Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble("total");
            }
        }
    }

    /** Check that all participations within range for volunteer are confirmed. */
    public boolean allConfirmedInRange(long volunteerId, LocalDate from, LocalDate to) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS total,
                       SUM(CASE WHEN p.confirmed THEN 1 ELSE 0 END) AS done
                  FROM participations p
                  JOIN events e ON e.id = p.event_id
                 WHERE p.volunteer_id = ?
                   AND e.datetime IS NOT NULL
                   AND e.datetime::date BETWEEN ? AND ?
                """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, volunteerId);
            ps.setDate(2, java.sql.Date.valueOf(from));
            ps.setDate(3, java.sql.Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                long total = rs.getLong("total");
                long done = rs.getLong("done");
                return total > 0 && total == done;
            }
        }
    }

    /** Hours worked per month for a volunteer (confirmed only). Key: yyyy-MM string. */
    public Map<String, Double> hoursByMonth(long volunteerId) throws SQLException {
        Map<String, Double> map = new LinkedHashMap<>();
        String sql = """
                SELECT TO_CHAR(DATE_TRUNC('month', e.datetime), 'YYYY-MM') AS ym,
                       SUM(p.hours_worked) AS hrs
                  FROM participations p
                  JOIN events e ON e.id = p.event_id
                 WHERE p.volunteer_id = ?
                   AND p.confirmed = TRUE
                   AND e.datetime IS NOT NULL
                 GROUP BY ym
                 ORDER BY ym
                """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, volunteerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("ym"), rs.getDouble("hrs"));
                }
            }
        }
        return map;
    }

    /** Строка для раздела «Информация о волонтёрах»: кто, на каком мероприятии был, когда и сколько часов. */
    public record VolunteerEventRow(long volunteerId, String fio, long eventId, String eventTitle,
                                    Timestamp datetime, double hours, boolean confirmed) {}

    /** Все участия с ФИО волонтёра и сведениями о мероприятии — для общего просмотра с фильтрацией. */
    public List<VolunteerEventRow> volunteerEventRows() throws SQLException {
        String sql = """
                SELECT v.id AS vid, v.fio, e.id AS eid, e.title, e.datetime,
                       p.hours_worked, p.confirmed
                  FROM participations p
                  JOIN volunteers v ON v.id = p.volunteer_id
                  JOIN events     e ON e.id = p.event_id
                 ORDER BY v.fio, e.datetime NULLS LAST
                """;
        List<VolunteerEventRow> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new VolunteerEventRow(
                        rs.getLong("vid"),
                        rs.getString("fio"),
                        rs.getLong("eid"),
                        rs.getString("title"),
                        rs.getTimestamp("datetime"),
                        rs.getDouble("hours_worked"),
                        rs.getBoolean("confirmed")));
            }
        }
        return list;
    }

    private Participation map(ResultSet rs) throws SQLException {
        Participation p = new Participation();
        p.setId(rs.getLong("id"));
        p.setVolunteerId(rs.getLong("volunteer_id"));
        p.setEventId(rs.getLong("event_id"));
        p.setHoursWorked(rs.getDouble("hours_worked"));
        p.setConfirmed(rs.getBoolean("confirmed"));
        long cbId = rs.getLong("confirmed_by_user_id");
        p.setConfirmedByUserId(rs.wasNull() ? null : cbId);
        p.setConfirmedByName(rs.getString("confirmed_by_name"));
        Timestamp ts = rs.getTimestamp("confirmed_at");
        p.setConfirmedAt(ts == null ? null : ts.toLocalDateTime());
        p.setVolunteerName(rs.getString("volunteer_name"));
        p.setEventTitle(rs.getString("event_title"));
        return p;
    }

    /** Устанавливает параметр-Long или NULL, если значение отсутствует. */
    private static void setLongOrNull(PreparedStatement ps, int idx, Long value) throws SQLException {
        if (value == null) ps.setNull(idx, java.sql.Types.BIGINT);
        else ps.setLong(idx, value);
    }
}
