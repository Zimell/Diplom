package org.example.volonterhoursapp.dao;

import org.example.volonterhoursapp.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Справочник организаторов мероприятий (таблица organizers).
 * Организатор хранится в events как внешний ключ organizer_id.
 */
public class OrganizerDAO {

    /** Список названий организаторов по алфавиту — для выпадающего списка в форме мероприятия. */
    public List<String> findAllNames() throws SQLException {
        List<String> list = new ArrayList<>();
        String sql = "SELECT name FROM organizers ORDER BY name";
        try (Connection c = DatabaseManager.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(rs.getString("name"));
        }
        return list;
    }

    /**
     * Возвращает id организатора с указанным названием, создавая запись в справочнике,
     * если такого организатора ещё нет. Названия уникальны (UNIQUE), поэтому повторного
     * дубликата не появится.
     */
    public long findOrCreate(String name) throws SQLException {
        String sql = "INSERT INTO organizers(name) VALUES (?) "
                + "ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name RETURNING id";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }
}
