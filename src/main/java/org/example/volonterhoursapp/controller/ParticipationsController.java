package org.example.volonterhoursapp.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import org.example.volonterhoursapp.dao.AuditDAO;
import org.example.volonterhoursapp.dao.EventDAO;
import org.example.volonterhoursapp.dao.ParticipationDAO;
import org.example.volonterhoursapp.dao.VolunteerDAO;
import org.example.volonterhoursapp.model.Event;
import org.example.volonterhoursapp.model.Participation;
import org.example.volonterhoursapp.model.Volunteer;
import org.example.volonterhoursapp.ui.CuratorSession;
import org.example.volonterhoursapp.ui.UiUtils;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class ParticipationsController implements Refreshable, StatusAware {

    @FXML private TableView<Participation> table;
    @FXML private TableColumn<Participation, Number> colId;
    @FXML private TableColumn<Participation, String> colVolunteer;
    @FXML private TableColumn<Participation, String> colEvent;
    @FXML private TableColumn<Participation, Number> colHours;
    @FXML private TableColumn<Participation, String> colStatus;
    @FXML private TableColumn<Participation, String> colConfirmedBy;

    @FXML private ComboBox<Volunteer> volunteerCombo;
    @FXML private ComboBox<Event> eventCombo;
    @FXML private TextField hoursField;
    @FXML private CheckBox onlyPending;
    @FXML private javafx.scene.control.Label statusLabel;

    private final ParticipationDAO dao = new ParticipationDAO();
    private final VolunteerDAO volunteerDAO = new VolunteerDAO();
    private final EventDAO eventDAO = new EventDAO();
    private final AuditDAO audit = new AuditDAO();

    private final ObservableList<Participation> all = FXCollections.observableArrayList();
    private FilteredList<Participation> filtered;
    private Consumer<String> status = s -> {};

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    private void initialize() {
        colId.setCellValueFactory(d -> d.getValue().idProperty());
        colVolunteer.setCellValueFactory(d -> d.getValue().volunteerNameProperty());
        colEvent.setCellValueFactory(d -> d.getValue().eventTitleProperty());
        colHours.setCellValueFactory(d -> d.getValue().hoursWorkedProperty());
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().isConfirmed() ? "Подтверждено" : "Ожидает"));
        colConfirmedBy.setCellValueFactory(d -> {
            Participation p = d.getValue();
            if (!p.isConfirmed()) return new SimpleStringProperty("");
            String by = p.getConfirmedByName() == null ? "" : p.getConfirmedByName();
            String at = p.getConfirmedAt() == null ? "" : " · " + p.getConfirmedAt().format(DT_FMT);
            return new SimpleStringProperty(by + at);
        });

        filtered = new FilteredList<>(all, p -> true);
        table.setItems(filtered);

        onlyPending.selectedProperty().addListener((o, a, b) ->
                filtered.setPredicate(b ? p -> !p.isConfirmed() : p -> true));

        volunteerCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Volunteer v) { return v == null ? "" : v.getFio() + " (#" + v.getId() + ")"; }
            @Override public Volunteer fromString(String s) { return null; }
        });
        eventCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Event e) {
                if (e == null) return "";
                return e.getTitle() + (e.getDatetime() == null ? "" : " · " + e.getDatetime().format(DT_FMT));
            }
            @Override public Event fromString(String s) { return null; }
        });

        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> populate(b));
    }

    @Override public void setStatusConsumer(Consumer<String> status) { this.status = status; }

    @Override
    public void refresh() {
        try {
            all.setAll(dao.findAll());
            volunteerCombo.getItems().setAll(volunteerDAO.findAll());
            eventCombo.getItems().setAll(eventDAO.findAll());
            status.accept("Загружено участий: " + all.size());
        } catch (SQLException ex) {
            status.accept("Ошибка: " + ex.getMessage());
        }
    }

    @FXML private void onNew() {
        table.getSelectionModel().clearSelection();
        volunteerCombo.setValue(null);
        eventCombo.setValue(null);
        hoursField.clear();
        statusLabel.setText("");
    }

    @FXML private void onSave() {
        Volunteer v = volunteerCombo.getValue();
        Event e = eventCombo.getValue();
        if (v == null || e == null) { UiUtils.error("Выберите волонтёра и мероприятие."); return; }
        String raw = hoursField.getText() == null ? "" : hoursField.getText().trim().replace(',', '.');
        if (raw.isEmpty()) { UiUtils.error("Укажите количество часов."); return; }
        double hours;
        try {
            hours = Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            UiUtils.error("Часы — число, например 4.5"); return;
        }
        if (hours < 0 || hours > 999.9) { UiUtils.error("Часы вне допустимого диапазона."); return; }

        try {
            Participation cur = table.getSelectionModel().getSelectedItem();
            if (cur == null) {
                Participation p = new Participation();
                p.setVolunteerId(v.getId());
                p.setEventId(e.getId());
                p.setHoursWorked(hours);
                p.setConfirmed(false);
                dao.insert(p);
                audit.log(CuratorSession.getUserId(), "participation.create",
                        v.getFio() + " → " + e.getTitle() + " (" + hours + "ч)");
                status.accept("Запись участия создана.");
            } else {
                cur.setVolunteerId(v.getId());
                cur.setEventId(e.getId());
                cur.setHoursWorked(hours);
                dao.update(cur);
                audit.log(CuratorSession.getUserId(), "participation.update",
                        "#" + cur.getId() + " → " + v.getFio() + " / " + e.getTitle());
                status.accept("Запись обновлена.");
            }
            refresh();
        } catch (SQLException ex) {
            UiUtils.error("Не удалось сохранить: " + ex.getMessage());
        }
    }

    @FXML private void onDelete() {
        Participation p = table.getSelectionModel().getSelectedItem();
        if (p == null) return;
        if (!UiUtils.confirm("Удалить запись участия #" + p.getId() + "?")) return;
        try {
            dao.delete(p.getId());
            audit.log(CuratorSession.getUserId(), "participation.delete", "#" + p.getId());
            refresh();
            onNew();
        } catch (SQLException ex) {
            UiUtils.error("Не удалось удалить: " + ex.getMessage());
        }
    }

    @FXML private void onConfirm() {
        Participation p = table.getSelectionModel().getSelectedItem();
        if (p == null) return;
        Long uid = CuratorSession.getUserId();
        if (uid == null) {
            UiUtils.error("Не удалось определить текущего пользователя — войдите заново."); return;
        }
        try {
            dao.confirm(p.getId(), uid);
            audit.log(uid, "participation.confirm",
                    p.getVolunteerName() + " / " + p.getEventTitle() + " (" + p.getHoursWorked() + "ч)");
            refresh();
            status.accept("Подтверждено: #" + p.getId());
        } catch (SQLException ex) {
            UiUtils.error("Не удалось подтвердить: " + ex.getMessage());
        }
    }

    @FXML private void onRevoke() {
        Participation p = table.getSelectionModel().getSelectedItem();
        if (p == null) return;
        if (!UiUtils.confirm("Снять подтверждение с записи #" + p.getId() + "?")) return;
        try {
            dao.revoke(p.getId());
            audit.log(CuratorSession.getUserId(), "participation.revoke", "#" + p.getId());
            refresh();
        } catch (SQLException ex) {
            UiUtils.error("Не удалось: " + ex.getMessage());
        }
    }

    private void populate(Participation p) {
        if (p == null) { statusLabel.setText(""); return; }
        for (Volunteer v : volunteerCombo.getItems()) if (v.getId() == p.getVolunteerId()) volunteerCombo.setValue(v);
        for (Event e : eventCombo.getItems()) if (e.getId() == p.getEventId()) eventCombo.setValue(e);
        hoursField.setText(String.valueOf(p.getHoursWorked()));
        statusLabel.setText(p.isConfirmed()
                ? "Подтверждено " + (p.getConfirmedByName() == null ? "" : p.getConfirmedByName())
                : "");
    }
}
