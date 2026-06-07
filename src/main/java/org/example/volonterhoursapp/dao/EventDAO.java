package org.example.volonterhoursapp.dao;

import org.example.volonterhoursapp.db.DatabaseManager;
import org.example.volonterhoursapp.model.Event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class EventDAO {

    private static final String SELECT_BASE = """
            SELECT e.id, e.title, e.datetime, e.location, e.type,
                   e.organizer_id, o.name AS organizer_name
              FROM events e
              JOIN organizers o ON o.id = e.organizer_id
            """;

    public List<Event> findAll() throws SQLException {
        List<Event> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(SELECT_BASE + " ORDER BY e.datetime DESC NULLS LAST")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Event findById(long id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BASE + " WHERE e.id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public long insert(Event e) throws SQLException {
        String sql = "INSERT INTO events(title, datetime, location, type, organizer_id) VALUES (?,?,?,?,?) RETURNING id";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, e.getTitle());
            if (e.getDatetime() != null) ps.setTimestamp(2, Timestamp.valueOf(e.getDatetime())); else ps.setNull(2, Types.TIMESTAMP);
            ps.setString(3, e.getLocation());
            ps.setString(4, e.getType());
            ps.setLong(5, e.getOrganizerId());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                long id = rs.getLong(1);
                e.setId(id);
                return id;
            }
        }
    }

    public void update(Event e) throws SQLException {
        String sql = "UPDATE events SET title=?, datetime=?, location=?, type=?, organizer_id=? WHERE id=?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, e.getTitle());
            if (e.getDatetime() != null) ps.setTimestamp(2, Timestamp.valueOf(e.getDatetime())); else ps.setNull(2, Types.TIMESTAMP);
            ps.setString(3, e.getLocation());
            ps.setString(4, e.getType());
            ps.setLong(5, e.getOrganizerId());
            ps.setLong(6, e.getId());
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM events WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public record PlannerRow(long eventId, String title, java.sql.Timestamp datetime,
                             String location, String type, String organizer,
                             long participants, long pending, double totalHours) {}

    public List<PlannerRow> plannerRows(boolean futureOnly) throws SQLException {
        String sql = """
                SELECT e.id, e.title, e.datetime, e.location, e.type, o.name AS organizer,
                       COUNT(p.id)                                                  AS participants,
                       SUM(CASE WHEN p.confirmed = FALSE THEN 1 ELSE 0 END)         AS pending,
                       COALESCE(SUM(CASE WHEN p.confirmed THEN p.hours_worked ELSE 0 END), 0) AS hours
                  FROM events e
                  JOIN organizers o ON o.id = e.organizer_id
                  LEFT JOIN participations p ON p.event_id = e.id
                 WHERE (? = FALSE) OR (e.datetime IS NOT NULL AND e.datetime >= CURRENT_TIMESTAMP)
                 GROUP BY e.id, o.name
                 ORDER BY e.datetime NULLS LAST
                """;
        List<PlannerRow> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, futureOnly);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PlannerRow(
                            rs.getLong("id"),
                            rs.getString("title"),
                            rs.getTimestamp("datetime"),
                            rs.getString("location"),
                            rs.getString("type"),
                            rs.getString("organizer"),
                            rs.getLong("participants"),
                            rs.getLong("pending"),
                            rs.getDouble("hours")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Строка личного дашборда волонтёра.
     * {@code participated} = true, если волонтёр уже отмечен на мероприятии (есть участие);
     * иначе это предстоящее мероприятие, на которое волонтёр может попасть.
     */
    public record VolunteerEventRow(long eventId, String title, Timestamp datetime,
                                    String location, String type,
                                    boolean participated, Double hours, Boolean confirmed) {}

    /**
     * Мероприятия для дашборда волонтёра: те, где он участвовал (с часами и статусом подтверждения),
     * плюс ещё не прошедшие мероприятия, на которые он может записаться.
     */
    public List<VolunteerEventRow> dashboardForVolunteer(long volunteerId) throws SQLException {
        String sql = """
                SELECT e.id, e.title, e.datetime, e.location, e.type,
                       p.id AS pid, p.hours_worked, p.confirmed
                  FROM events e
                  LEFT JOIN participations p
                         ON p.event_id = e.id AND p.volunteer_id = ?
                 WHERE p.id IS NOT NULL
                    OR (e.datetime IS NOT NULL AND e.datetime >= CURRENT_TIMESTAMP)
                 ORDER BY e.datetime NULLS LAST
                """;
        List<VolunteerEventRow> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, volunteerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long pid = rs.getLong("pid");
                    boolean participated = !rs.wasNull();
                    Double hours = null;
                    Boolean confirmed = null;
                    if (participated) {
                        hours = rs.getDouble("hours_worked");
                        confirmed = rs.getBoolean("confirmed");
                    }
                    list.add(new VolunteerEventRow(
                            rs.getLong("id"),
                            rs.getString("title"),
                            rs.getTimestamp("datetime"),
                            rs.getString("location"),
                            rs.getString("type"),
                            participated, hours, confirmed));
                }
            }
        }
        return list;
    }

    private Event map(ResultSet rs) throws SQLException {
        Event e = new Event();
        e.setId(rs.getLong("id"));
        e.setTitle(rs.getString("title"));
        Timestamp ts = rs.getTimestamp("datetime");
        e.setDatetime(ts == null ? null : ts.toLocalDateTime());
        e.setLocation(rs.getString("location"));
        e.setType(rs.getString("type"));
        e.setOrganizerId(rs.getLong("organizer_id"));
        e.setOrganizerName(rs.getString("organizer_name"));
        return e;
    }
}
