package org.example.volonterhoursapp.service;

import org.example.volonterhoursapp.db.DatabaseManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class StatisticsService {

    public record Snapshot(long volunteers, long events, long participations,
                           long pending, double totalHours) {}

    public Snapshot snapshot() throws SQLException {
        String sql = """
                SELECT
                  (SELECT COUNT(*) FROM volunteers)                                  AS volunteers,
                  (SELECT COUNT(*) FROM events)                                      AS events,
                  (SELECT COUNT(*) FROM participations)                              AS participations,
                  (SELECT COUNT(*) FROM participations WHERE confirmed = FALSE)      AS pending,
                  (SELECT COALESCE(SUM(hours_worked),0) FROM participations
                     WHERE confirmed = TRUE)                                         AS total_hours
                """;
        try (Connection c = DatabaseManager.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return new Snapshot(
                    rs.getLong("volunteers"),
                    rs.getLong("events"),
                    rs.getLong("participations"),
                    rs.getLong("pending"),
                    rs.getDouble("total_hours")
            );
        }
    }
}
