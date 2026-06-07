package org.example.volonterhoursapp.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.example.volonterhoursapp.dao.AuditDAO;
import org.example.volonterhoursapp.dao.EventDAO;
import org.example.volonterhoursapp.dao.OrganizerDAO;
import org.example.volonterhoursapp.model.Event;
import org.example.volonterhoursapp.ui.CuratorSession;
import org.example.volonterhoursapp.ui.UiUtils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;

public class EventsController implements Refreshable, StatusAware {

    @FXML private SplitPane splitPane;
    @FXML private VBox editorCard;
    @FXML private TableView<Event> table;
    @FXML private TableColumn<Event, Number> colId;
    @FXML private TableColumn<Event, String> colTitle;
    @FXML private TableColumn<Event, String> colDateTime;
    @FXML private TableColumn<Event, String> colLocation;
    @FXML private TableColumn<Event, String> colType;
    @FXML private TableColumn<Event, String> colOrganizer;

    @FXML private TextField titleField;
    @FXML private DatePicker datePicker;
    @FXML private TextField timeField;
    @FXML private TextField locationField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> organizerCombo;

    private final EventDAO dao = new EventDAO();
    private final OrganizerDAO organizerDAO = new OrganizerDAO();
    private final AuditDAO audit = new AuditDAO();
    private final ObservableList<Event> items = FXCollections.observableArrayList();
    private Consumer<String> status = s -> {};

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private void initialize() {
        colId.setCellValueFactory(d -> d.getValue().idProperty());
        colTitle.setCellValueFactory(d -> d.getValue().titleProperty());
        colDateTime.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDatetime() == null ? "" : d.getValue().getDatetime().format(DT_FMT)));
        colLocation.setCellValueFactory(d -> d.getValue().locationProperty());
        colType.setCellValueFactory(d -> d.getValue().typeProperty());
        colOrganizer.setCellValueFactory(d -> d.getValue().organizerNameProperty());

        table.setItems(items);
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> populate(b));

        // Волонтёр (роль USER) может только просматривать — убираем карточку редактирования.
        if (!CuratorSession.isAdmin() && !CuratorSession.isCurator()) {
            splitPane.getItems().remove(editorCard);
        }
    }

    @Override public void setStatusConsumer(Consumer<String> status) { this.status = status; }

    @Override
    public void refresh() {
        try {
            organizerCombo.getItems().setAll(organizerDAO.findAllNames());
            items.setAll(dao.findAll());
            status.accept("Загружено мероприятий: " + items.size());
        } catch (SQLException ex) {
            status.accept("Ошибка: " + ex.getMessage());
        }
    }

    @FXML private void onNew() {
        table.getSelectionModel().clearSelection();
        titleField.clear();
        datePicker.setValue(null);
        timeField.clear();
        locationField.clear();
        typeCombo.setValue(null);
        organizerCombo.setValue(null);
        organizerCombo.getEditor().clear();
        titleField.requestFocus();
    }

    @FXML private void onSave() {
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        if (title.isEmpty()) { UiUtils.error("Название обязательно."); return; }

        LocalDateTime dt;
        try {
            dt = parseDateTimeOrNull();
        } catch (DateTimeParseException ex) {
            UiUtils.error("Время в формате HH:mm.");
            return;
        }

        String organizerName = organizerName();
        if (organizerName.isEmpty()) { UiUtils.error("Укажите организатора."); return; }

        Event current = table.getSelectionModel().getSelectedItem();
        try {
            long organizerId = organizerDAO.findOrCreate(organizerName);
            if (current == null) {
                Event e = new Event();
                fill(e, title, dt, organizerId, organizerName);
                dao.insert(e);
                audit.log(CuratorSession.getUserId(), "event.create", e.getTitle() + " (#" + e.getId() + ")");
                status.accept("Создано мероприятие #" + e.getId());
            } else {
                fill(current, title, dt, organizerId, organizerName);
                dao.update(current);
                audit.log(CuratorSession.getUserId(), "event.update", current.getTitle() + " (#" + current.getId() + ")");
                status.accept("Сохранены изменения #" + current.getId());
            }
            refresh();
        } catch (SQLException ex) {
            UiUtils.error("Не удалось сохранить: " + ex.getMessage());
        }
    }

    @FXML private void onDelete() {
        Event e = table.getSelectionModel().getSelectedItem();
        if (e == null) { status.accept("Выберите мероприятие."); return; }
        if (!UiUtils.confirm("Удалить мероприятие «" + e.getTitle() + "»? Связанные участия будут удалены.")) return;
        try {
            dao.delete(e.getId());
            audit.log(CuratorSession.getUserId(), "event.delete", e.getTitle() + " (#" + e.getId() + ")");
            refresh();
            onNew();
        } catch (SQLException ex) {
            UiUtils.error("Не удалось удалить: " + ex.getMessage());
        }
    }

    private void fill(Event e, String title, LocalDateTime dt, long organizerId, String organizerName) {
        e.setTitle(title);
        e.setDatetime(dt);
        e.setLocation(text(locationField));
        e.setType(typeCombo.getValue());
        e.setOrganizerId(organizerId);
        e.setOrganizerName(organizerName);
    }

    /** Введённое или выбранное название организатора (для редактируемого ComboBox). */
    private String organizerName() {
        String typed = organizerCombo.getEditor() == null ? null : organizerCombo.getEditor().getText();
        String s = (typed != null && !typed.isBlank()) ? typed : organizerCombo.getValue();
        return s == null ? "" : s.trim();
    }

    private LocalDateTime parseDateTimeOrNull() {
        LocalDate date = datePicker.getValue();
        String t = timeField.getText() == null ? "" : timeField.getText().trim();
        if (date == null) return null;
        if (t.isEmpty()) return date.atStartOfDay();
        LocalTime time = LocalTime.parse(t, TIME_FMT);
        return date.atTime(time);
    }

    private void populate(Event e) {
        if (e == null) return;
        titleField.setText(e.getTitle());
        datePicker.setValue(e.getDatetime() == null ? null : e.getDatetime().toLocalDate());
        timeField.setText(e.getDatetime() == null ? "" : e.getDatetime().toLocalTime().format(TIME_FMT));
        locationField.setText(e.getLocation());
        typeCombo.setValue(e.getType());
        organizerCombo.setValue(e.getOrganizerName());
    }

    private static String text(TextField f) {
        String s = f.getText();
        return s == null ? null : s.trim();
    }
}
