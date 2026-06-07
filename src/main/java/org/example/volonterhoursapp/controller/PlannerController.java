package org.example.volonterhoursapp.controller;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import org.example.volonterhoursapp.dao.AuditDAO;
import org.example.volonterhoursapp.dao.EventDAO;
import org.example.volonterhoursapp.dao.ParticipationDAO;
import org.example.volonterhoursapp.dao.VolunteerDAO;
import org.example.volonterhoursapp.model.Participation;
import org.example.volonterhoursapp.model.Volunteer;
import org.example.volonterhoursapp.ui.CuratorSession;
import org.example.volonterhoursapp.ui.UiUtils;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class PlannerController implements Refreshable, StatusAware {

    @FXML private CheckBox futureOnly;
    @FXML private TableView<Row> table;
    @FXML private TableColumn<Row, String> colTitle;
    @FXML private TableColumn<Row, String> colDateTime;
    @FXML private TableColumn<Row, String> colLocation;
    @FXML private TableColumn<Row, String> colType;
    @FXML private TableColumn<Row, Number> colParticipants;
    @FXML private TableColumn<Row, Number> colPending;
    @FXML private TableColumn<Row, Number> colHours;
    @FXML private ComboBox<Volunteer> volunteerCombo;
    @FXML private TextField hoursField;
    @FXML private Label selectedEventLabel;

    private final EventDAO eventDAO = new EventDAO();
    private final VolunteerDAO volunteerDAO = new VolunteerDAO();
    private final ParticipationDAO participationDAO = new ParticipationDAO();
    private final AuditDAO audit = new AuditDAO();

    private final ObservableList<Row> items = FXCollections.observableArrayList();
    private Consumer<String> status = s -> {};

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static class Row {
        public final SimpleLongProperty   eventId   = new SimpleLongProperty();
        public final SimpleStringProperty title     = new SimpleStringProperty();
        public final SimpleStringProperty datetime  = new SimpleStringProperty();
        public final SimpleStringProperty location  = new SimpleStringProperty();
        public final SimpleStringProperty type      = new SimpleStringProperty();
        public final SimpleLongProperty   parts     = new SimpleLongProperty();
        public final SimpleLongProperty   pending   = new SimpleLongProperty();
        public final SimpleDoubleProperty hours     = new SimpleDoubleProperty();
    }

    @FXML
    private void initialize() {
        colTitle.setCellValueFactory(d -> d.getValue().title);
        colDateTime.setCellValueFactory(d -> d.getValue().datetime);
        colLocation.setCellValueFactory(d -> d.getValue().location);
        colType.setCellValueFactory(d -> d.getValue().type);
        colParticipants.setCellValueFactory(d -> d.getValue().parts);
        colPending.setCellValueFactory(d -> d.getValue().pending);
        colHours.setCellValueFactory(d -> d.getValue().hours);

        volunteerCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Volunteer v) { return v == null ? "" : v.getFio() + " (#" + v.getId() + ")"; }
            @Override public Volunteer fromString(String s) { return null; }
        });

        table.setItems(items);
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            selectedEventLabel.setText(b == null
                    ? "Мероприятие не выбрано."
                    : "Выбрано: " + b.title.get() + (b.datetime.get().isEmpty() ? "" : " · " + b.datetime.get()));
        });
    }

    @Override public void setStatusConsumer(Consumer<String> status) { this.status = status; }

    @Override
    public void refresh() {
        try {
            volunteerCombo.getItems().setAll(volunteerDAO.findAll());
            load();
        } catch (SQLException ex) {
            status.accept("Ошибка: " + ex.getMessage());
        }
    }

    @FXML private void onRefresh() { refresh(); }

    @FXML private void onFilter() { load(); }

    private void load() {
        try {
            items.clear();
            for (EventDAO.PlannerRow r : eventDAO.plannerRows(futureOnly.isSelected())) {
                Row row = new Row();
                row.eventId.set(r.eventId());
                row.title.set(r.title());
                row.datetime.set(r.datetime() == null ? "" : r.datetime().toLocalDateTime().format(DT_FMT));
                row.location.set(r.location());
                row.type.set(r.type());
                row.parts.set(r.participants());
                row.pending.set(r.pending());
                row.hours.set(r.totalHours());
                items.add(row);
            }
            status.accept("Мероприятий в плане: " + items.size());
        } catch (SQLException ex) {
            status.accept("Ошибка: " + ex.getMessage());
        }
    }

    @FXML private void onEnroll() {
        Row r = table.getSelectionModel().getSelectedItem();
        if (r == null) { UiUtils.error("Выберите мероприятие в таблице."); return; }
        Volunteer v = volunteerCombo.getValue();
        if (v == null) { UiUtils.error("Выберите волонтёра."); return; }
        String raw = hoursField.getText() == null ? "" : hoursField.getText().trim().replace(',', '.');
        if (raw.isEmpty()) { UiUtils.error("Укажите плановое количество часов."); return; }
        double hrs;
        try {
            hrs = Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            UiUtils.error("Часы — число."); return;
        }
        try {
            Participation p = new Participation();
            p.setVolunteerId(v.getId());
            p.setEventId(r.eventId.get());
            p.setHoursWorked(hrs);
            p.setConfirmed(false);
            participationDAO.insert(p);
            audit.log(CuratorSession.getUserId(), "planner.enroll",
                    v.getFio() + " → " + r.title.get() + " (" + hrs + "ч)");
            status.accept("Записан волонтёр на «" + r.title.get() + "».");
            load();
        } catch (SQLException ex) {
            UiUtils.error("Не удалось записать: " + ex.getMessage());
        }
    }
}
