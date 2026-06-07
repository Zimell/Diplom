package org.example.volonterhoursapp.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.example.volonterhoursapp.dao.ParticipationDAO;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Общая информация о волонтёрах: кто на каких мероприятиях участвовал.
 * Доступна для просмотра всем ролям, поддерживает фильтрацию по ФИО и названию мероприятия.
 */
public class VolunteerInfoController implements Refreshable, StatusAware {

    @FXML private TextField searchField;
    @FXML private TableView<ParticipationDAO.VolunteerEventRow> table;
    @FXML private TableColumn<ParticipationDAO.VolunteerEventRow, String> colVolunteer;
    @FXML private TableColumn<ParticipationDAO.VolunteerEventRow, String> colEvent;
    @FXML private TableColumn<ParticipationDAO.VolunteerEventRow, String> colDate;
    @FXML private TableColumn<ParticipationDAO.VolunteerEventRow, String> colHours;
    @FXML private TableColumn<ParticipationDAO.VolunteerEventRow, String> colStatus;

    private final ParticipationDAO dao = new ParticipationDAO();
    private final ObservableList<ParticipationDAO.VolunteerEventRow> items = FXCollections.observableArrayList();
    private final List<ParticipationDAO.VolunteerEventRow> all = new ArrayList<>();
    private Consumer<String> status = s -> {};

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    private void initialize() {
        colVolunteer.setCellValueFactory(d -> new SimpleStringProperty(nz(d.getValue().fio())));
        colEvent.setCellValueFactory(d -> new SimpleStringProperty(nz(d.getValue().eventTitle())));
        colDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().datetime() == null ? "—" :
                        d.getValue().datetime().toLocalDateTime().format(DT_FMT)));
        colHours.setCellValueFactory(d -> new SimpleStringProperty(
                String.format(Locale.ROOT, "%.1f", d.getValue().hours())));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().confirmed() ? "Подтверждено" : "Не подтверждено"));
        table.setItems(items);
    }

    @Override public void setStatusConsumer(Consumer<String> status) { this.status = status; }

    @Override
    public void refresh() {
        try {
            all.clear();
            all.addAll(dao.volunteerEventRows());
            applyFilter();
        } catch (SQLException ex) {
            status.accept("Ошибка: " + ex.getMessage());
        }
    }

    @FXML private void onSearch() { applyFilter(); }

    @FXML private void onReset() {
        searchField.clear();
        applyFilter();
    }

    private void applyFilter() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        List<ParticipationDAO.VolunteerEventRow> filtered = new ArrayList<>();
        for (ParticipationDAO.VolunteerEventRow r : all) {
            if (q.isEmpty()
                    || nz(r.fio()).toLowerCase().contains(q)
                    || nz(r.eventTitle()).toLowerCase().contains(q)) {
                filtered.add(r);
            }
        }
        items.setAll(filtered);
        status.accept("Записей: " + items.size());
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
