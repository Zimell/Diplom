package org.example.volonterhoursapp.dao;

import org.example.volonterhoursapp.db.DatabaseManager;
import org.example.volonterhoursapp.model.Volunteer;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class VolunteerDAO {

    public List<Volunteer> findAll() throws SQLException {
        List<Volunteer> list = new ArrayList<>();
        String sql = "SELECT id, fio, birth_date, phone, email, registered_at FROM volunteers ORDER BY fio";
        try (Connection c = DatabaseManager.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public List<Volunteer> search(String query) throws SQLException {
        List<Volunteer> list = new ArrayList<>();
        String sql = """
                SELECT id, fio, birth_date, phone, email, registered_at
                FROM volunteers
                WHERE LOWER(fio) LIKE ? OR LOWER(COALESCE(email,'')) LIKE ? OR LOWER(COALESCE(phone,'')) LIKE ?
                ORDER BY fio
                """;
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            String q = "%" + query.toLowerCase() + "%";
            ps.setString(1, q);
            ps.setString(2, q);
            ps.setString(3, q);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** Волонтёр по точному ФИО — связывает учётную запись USER с его карточкой волонтёра. */
    public Volunteer findByFio(String fio) throws SQLException {
        if (fio == null || fio.isBlank()) return null;
        String sql = "SELECT id, fio, birth_date, phone, email, registered_at "
                + "FROM volunteers WHERE LOWER(fio) = LOWER(?) ORDER BY id LIMIT 1";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fio.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Volunteer findById(long id) throws SQLException {
        String sql = "SELECT id, fio, birth_date, phone, email, registered_at FROM volunteers WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public long insert(Volunteer v) throws SQLException {
        String sql = "INSERT INTO volunteers(fio, birth_date, phone, email, registered_at) VALUES (?,?,?,?,?) RETURNING id";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, v.getFio());
            if (v.getBirthDate() != null) ps.setDate(2, Date.valueOf(v.getBirthDate())); else ps.setNull(2, Types.DATE);
            ps.setString(3, v.getPhone());
            ps.setString(4, v.getEmail());
            if (v.getRegisteredAt() != null) ps.setDate(5, Date.valueOf(v.getRegisteredAt())); else ps.setNull(5, Types.DATE);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                long id = rs.getLong(1);
                v.setId(id);
                return id;
            }
        }
    }

    public void update(Volunteer v) throws SQLException {
        String sql = "UPDATE volunteers SET fio=?, birth_date=?, phone=?, email=?, registered_at=? WHERE id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, v.getFio());
            if (v.getBirthDate() != null) ps.setDate(2, Date.valueOf(v.getBirthDate())); else ps.setNull(2, Types.DATE);
            ps.setString(3, v.getPhone());
            ps.setString(4, v.getEmail());
            if (v.getRegisteredAt() != null) ps.setDate(5, Date.valueOf(v.getRegisteredAt())); else ps.setNull(5, Types.DATE);
            ps.setLong(6, v.getId());
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM volunteers WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public record RankingRow(long volunteerId, String fio, double totalHours,
                             long eventsCount, java.sql.Timestamp lastActivity) {}

    public List<RankingRow> ranking(java.time.LocalDate from, java.time.LocalDate to) throws SQLException {
        // Period filter: when from/to are NULL, every confirmed participation counts;
        // otherwise we require the event to have a datetime within the range.
        String sql = """
                SELECT v.id, v.fio,
                       COALESCE((
                           SELECT SUM(p.hours_worked)
                             FROM participations p
                             JOIN events ev ON ev.id = p.event_id
                            WHERE p.volunteer_id = v.id
                              AND p.confirmed = TRUE
                              AND (? IS NULL OR (ev.datetime IS NOT NULL AND ev.datetime::date >= ?))
                              AND (? IS NULL OR (ev.datetime IS NOT NULL AND ev.datetime::date <= ?))
                       ), 0) AS total_hours,
                       COALESCE((
                           SELECT COUNT(DISTINCT p.event_id)
                             FROM participations p
                             JOIN events ev ON ev.id = p.event_id
                            WHERE p.volunteer_id = v.id
                              AND p.confirmed = TRUE
                              AND (? IS NULL OR (ev.datetime IS NOT NULL AND ev.datetime::date >= ?))
                              AND (? IS NULL OR (ev.datetime IS NOT NULL AND ev.datetime::date <= ?))
                       ), 0) AS events_count,
                       (SELECT MAX(p.confirmed_at) FROM participations p WHERE p.volunteer_id = v.id) AS last_activity
                  FROM volunteers v
                 ORDER BY total_hours DESC, v.fio
                """;
        List<RankingRow> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int[] fromIdx = {1, 2, 5, 6};
            int[] toIdx   = {3, 4, 7, 8};
            for (int idx : fromIdx) {
                if (from != null) ps.setDate(idx, Date.valueOf(from));
                else              ps.setNull(idx, Types.DATE);
            }
            for (int idx : toIdx) {
                if (to != null) ps.setDate(idx, Date.valueOf(to));
                else            ps.setNull(idx, Types.DATE);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new RankingRow(
                            rs.getLong("id"),
                            rs.getString("fio"),
                            rs.getDouble("total_hours"),
                            rs.getLong("events_count"),
                            rs.getTimestamp("last_activity")
                    ));
                }
            }
        }
        return list;
    }

    private Volunteer map(ResultSet rs) throws SQLException {
        Volunteer v = new Volunteer();
        v.setId(rs.getLong("id"));
        v.setFio(rs.getString("fio"));
        Date bd = rs.getDate("birth_date");
        v.setBirthDate(bd == null ? null : bd.toLocalDate());
        v.setPhone(rs.getString("phone"));
        v.setEmail(rs.getString("email"));
        Date ra = rs.getDate("registered_at");
        v.setRegisteredAt(ra == null ? null : ra.toLocalDate());
        return v;
    }
}
