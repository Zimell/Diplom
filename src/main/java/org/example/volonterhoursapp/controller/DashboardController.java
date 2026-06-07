package org.example.volonterhoursapp.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.volonterhoursapp.dao.AuditDAO;
import org.example.volonterhoursapp.dao.EventDAO;
import org.example.volonterhoursapp.dao.VolunteerDAO;
import org.example.volonterhoursapp.model.User;
import org.example.volonterhoursapp.model.Volunteer;
import org.example.volonterhoursapp.service.StatisticsService;
import org.example.volonterhoursapp.ui.AuditActions;
import org.example.volonterhoursapp.ui.CuratorSession;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

public class DashboardController implements Refreshable, StatusAware {

    @FXML private Label headerSubtitle;
    @FXML private VBox adminBox;
    @FXML private VBox volunteerBox;

    @FXML private FlowPane kpiPane;
    @FXML private TableView<AuditDAO.LogEntry> logTable;
    @FXML private TableColumn<AuditDAO.LogEntry, String> colTime;
    @FXML private TableColumn<AuditDAO.LogEntry, String> colActor;
    @FXML private TableColumn<AuditDAO.LogEntry, String> colAction;
    @FXML private TableColumn<AuditDAO.LogEntry, String> colDetails;

    @FXML private TableView<EventDAO.VolunteerEventRow> myEventsTable;
    @FXML private TableColumn<EventDAO.VolunteerEventRow, String> colMyDate;
    @FXML private TableColumn<EventDAO.VolunteerEventRow, String> colMyTitle;
    @FXML private TableColumn<EventDAO.VolunteerEventRow, String> colMyLocation;
    @FXML private TableColumn<EventDAO.VolunteerEventRow, String> colMyType;
    @FXML private TableColumn<EventDAO.VolunteerEventRow, String> colMyStatus;
    @FXML private TableColumn<EventDAO.VolunteerEventRow, String> colMyHours;

    private final StatisticsService stats = new StatisticsService();
    private final AuditDAO auditDAO = new AuditDAO();
    private final EventDAO eventDAO = new EventDAO();
    private final VolunteerDAO volunteerDAO = new VolunteerDAO();
    private Consumer<String> status = s -> {};

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Label kVolunteers = kpiValue();
    private final Label kEvents     = kpiValue();
    private final Label kPart       = kpiValue();
    private final Label kPending    = kpiValue();
    private final Label kHours      = kpiValue();

    private static Label kpiValue() {
        Label l = new Label("0");
        l.getStyleClass().add("kpi-value");
        return l;
    }

    /** Волонтёр (роль USER) видит только свои мероприятия; админ и куратор — общую сводку. */
    private boolean isVolunteer() {
        return !CuratorSession.isAdmin() && !CuratorSession.isCurator();
    }

    @FXML
    private void initialize() {
        kpiPane.getChildren().setAll(
                card("Волонтёры",              kVolunteers),
                card("Мероприятия",            kEvents),
                card("Записи участия",         kPart),
                card("Ожидают подтверждения",  kPending),
                card("Подтверждено часов",     kHours)
        );

        colTime.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().occurredAt() == null ? "" :
                        d.getValue().occurredAt().toLocalDateTime().format(TS_FMT)));
        colActor.setCellValueFactory(d -> new SimpleStringProperty(nz(d.getValue().actor())));
        colAction.setCellValueFactory(d -> new SimpleStringProperty(AuditActions.humanize(d.getValue().action())));
        colDetails.setCellValueFactory(d -> new SimpleStringProperty(nz(d.getValue().details())));

        colMyDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().datetime() == null ? "—" :
                        d.getValue().datetime().toLocalDateTime().format(DT_FMT)));
        colMyTitle.setCellValueFactory(d -> new SimpleStringProperty(nz(d.getValue().title())));
        colMyLocation.setCellValueFactory(d -> new SimpleStringProperty(nz(d.getValue().location())));
        colMyType.setCellValueFactory(d -> new SimpleStringProperty(nz(d.getValue().type())));
        colMyStatus.setCellValueFactory(d -> new SimpleStringProperty(statusText(d.getValue())));
        colMyHours.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().participated() && d.getValue().hours() != null
                        ? String.format(Locale.ROOT, "%.1f", d.getValue().hours()) : "—"));

        boolean volunteer = isVolunteer();
        toggle(adminBox, !volunteer);
        toggle(volunteerBox, volunteer);
        headerSubtitle.setText(volunteer
                ? "Мероприятия, в которых вы участвовали, и те, на которые можно записаться."
                : "Сводные показатели и последние действия.");
    }

    @Override
    public void setStatusConsumer(Consumer<String> status) {
        this.status = status;
    }

    @Override
    public void refresh() {
        if (isVolunteer()) refreshVolunteer();
        else refreshAdmin();
    }

    private void refreshAdmin() {
        try {
            StatisticsService.Snapshot s = stats.snapshot();
            kVolunteers.setText(String.valueOf(s.volunteers()));
            kEvents.setText(String.valueOf(s.events()));
            kPart.setText(String.valueOf(s.participations()));
            kPending.setText(String.valueOf(s.pending()));
            kHours.setText(String.format(Locale.ROOT, "%.1f", s.totalHours()));
            // Куратор видит в журнале только действия кураторов и волонтёров (без админов).
            logTable.getItems().setAll(auditDAO.findRecent(100, CuratorSession.isCurator()));
            status.accept("Сводка обновлена.");
        } catch (SQLException ex) {
            status.accept("Ошибка: " + ex.getMessage());
        }
    }

    private void refreshVolunteer() {
        try {
            User u = CuratorSession.getUser();
            Volunteer v = u == null ? null : volunteerDAO.findByFio(u.getFullName());
            if (v == null) {
                myEventsTable.getItems().clear();
                status.accept("Ваша карточка волонтёра не найдена — обратитесь к куратору.");
                return;
            }
            myEventsTable.getItems().setAll(eventDAO.dashboardForVolunteer(v.getId()));
            status.accept("Загружено мероприятий: " + myEventsTable.getItems().size());
        } catch (SQLException ex) {
            status.accept("Ошибка: " + ex.getMessage());
        }
    }

    private static String statusText(EventDAO.VolunteerEventRow r) {
        if (!r.participated()) return "Можно записаться";
        return Boolean.TRUE.equals(r.confirmed()) ? "Участие подтверждено" : "Ожидает подтверждения";
    }

    private static void toggle(Region node, boolean on) {
        node.setVisible(on);
        node.setManaged(on);
    }

    private static Region card(String title, Label value) {
        Label name = new Label(title);
        name.getStyleClass().add("kpi-label");
        VBox box = new VBox(6, value, name);
        box.getStyleClass().add("kpi-card");
        box.setAlignment(Pos.TOP_LEFT);
        return box;
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
