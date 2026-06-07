package org.example.volonterhoursapp.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import org.example.volonterhoursapp.dao.AuditDAO;
import org.example.volonterhoursapp.ui.AuditActions;
import org.example.volonterhoursapp.ui.CsvUtil;
import org.example.volonterhoursapp.ui.CuratorSession;
import org.example.volonterhoursapp.ui.UiUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AuditController implements Refreshable, StatusAware {

    @FXML private TextField actorField;
    @FXML private TextField actionField;
    @FXML private TableView<AuditDAO.LogEntry> table;
    @FXML private TableColumn<AuditDAO.LogEntry, String> colTime;
    @FXML private TableColumn<AuditDAO.LogEntry, String> colActor;
    @FXML private TableColumn<AuditDAO.LogEntry, String> colAction;
    @FXML private TableColumn<AuditDAO.LogEntry, String> colDetails;

    private final AuditDAO dao = new AuditDAO();
    private final ObservableList<AuditDAO.LogEntry> items = FXCollections.observableArrayList();
    private final List<AuditDAO.LogEntry> all = new ArrayList<>();
    private Consumer<String> status = s -> {};

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    private void initialize() {
        colTime.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().occurredAt() == null ? "" :
                        d.getValue().occurredAt().toLocalDateTime().format(TS_FMT)));
        colActor.setCellValueFactory(d -> new SimpleStringProperty(nz(d.getValue().actor())));
        colAction.setCellValueFactory(d -> new SimpleStringProperty(AuditActions.humanize(d.getValue().action())));
        colDetails.setCellValueFactory(d -> new SimpleStringProperty(nz(d.getValue().details())));
        table.setItems(items);
    }

    @Override public void setStatusConsumer(Consumer<String> status) { this.status = status; }

    @Override
    public void refresh() {
        try {
            all.clear();
            all.addAll(dao.findRecent(500, CuratorSession.isCurator()));
            applyFilter();
        } catch (SQLException ex) {
            status.accept("Ошибка: " + ex.getMessage());
        }
    }

    @FXML private void onSearch() { applyFilter(); }

    @FXML private void onReset() {
        actorField.clear();
        actionField.clear();
        applyFilter();
    }

    private void applyFilter() {
        String actorQ  = lower(actorField.getText());
        String actionQ = lower(actionField.getText());

        List<AuditDAO.LogEntry> filtered = new ArrayList<>();
        for (AuditDAO.LogEntry e : all) {
            String actor = nz(e.actor()).toLowerCase();
            String actionHuman = AuditActions.humanize(e.action()).toLowerCase();
            String actionCode  = nz(e.action()).toLowerCase();
            if (!actorQ.isEmpty() && !actor.contains(actorQ)) continue;
            if (!actionQ.isEmpty() && !actionHuman.contains(actionQ) && !actionCode.contains(actionQ)) continue;
            filtered.add(e);
        }
        items.setAll(filtered);
        status.accept("Записей в журнале: " + items.size());
    }

    @FXML private void onExportCsv() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Экспорт журнала");
        fc.setInitialFileName("audit.csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        var f = fc.showSaveDialog(table.getScene().getWindow());
        if (f == null) return;
        try (FileWriter w = new FileWriter(f, StandardCharsets.UTF_8)) {
            w.write(CsvUtil.BOM);
            w.write(CsvUtil.line("occurred_at", "actor", "action", "details"));
            for (AuditDAO.LogEntry e : items) {
                w.write(CsvUtil.line(
                        e.occurredAt() == null ? "" : e.occurredAt().toLocalDateTime().format(TS_FMT),
                        e.actor(),
                        AuditActions.humanize(e.action()),
                        e.details()));
            }
            status.accept("Экспортировано: " + f.getName());
        } catch (IOException ex) {
            UiUtils.error("Не удалось экспортировать: " + ex.getMessage());
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static String lower(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}
