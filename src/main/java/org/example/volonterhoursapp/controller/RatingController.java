package org.example.volonterhoursapp.controller;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import org.example.volonterhoursapp.dao.VolunteerDAO;
import org.example.volonterhoursapp.ui.CsvUtil;
import org.example.volonterhoursapp.ui.UiUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class RatingController implements Refreshable, StatusAware {

    @FXML private DatePicker fromPicker;
    @FXML private DatePicker toPicker;
    @FXML private TableView<Row> table;
    @FXML private TableColumn<Row, Number> colRank;
    @FXML private TableColumn<Row, String> colFio;
    @FXML private TableColumn<Row, Number> colHours;
    @FXML private TableColumn<Row, Number> colEvents;
    @FXML private TableColumn<Row, String> colLast;

    private final VolunteerDAO dao = new VolunteerDAO();
    private final ObservableList<Row> items = FXCollections.observableArrayList();
    private Consumer<String> status = s -> {};

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static class Row {
        public final SimpleIntegerProperty rank = new SimpleIntegerProperty();
        public final SimpleLongProperty   volunteerId = new SimpleLongProperty();
        public final SimpleStringProperty fio = new SimpleStringProperty();
        public final SimpleDoubleProperty hours = new SimpleDoubleProperty();
        public final SimpleLongProperty   events = new SimpleLongProperty();
        public final SimpleStringProperty last = new SimpleStringProperty();
    }

    @FXML
    private void initialize() {
        colRank.setCellValueFactory(d -> d.getValue().rank);
        colFio.setCellValueFactory(d -> d.getValue().fio);
        colHours.setCellValueFactory(d -> d.getValue().hours);
        colEvents.setCellValueFactory(d -> d.getValue().events);
        colLast.setCellValueFactory(d -> d.getValue().last);
        table.setItems(items);
    }

    @Override public void setStatusConsumer(Consumer<String> status) { this.status = status; }

    @Override
    public void refresh() {
        load();
    }

    @FXML private void onApply() { load(); }

    @FXML private void onReset() {
        fromPicker.setValue(null);
        toPicker.setValue(null);
        load();
    }

    private void load() {
        try {
            List<VolunteerDAO.RankingRow> rows = dao.ranking(fromPicker.getValue(), toPicker.getValue());
            items.clear();
            int i = 1;
            for (VolunteerDAO.RankingRow r : rows) {
                Row row = new Row();
                row.rank.set(i++);
                row.volunteerId.set(r.volunteerId());
                row.fio.set(r.fio());
                row.hours.set(r.totalHours());
                row.events.set(r.eventsCount());
                row.last.set(r.lastActivity() == null ? "" : r.lastActivity().toLocalDateTime().format(DT_FMT));
                items.add(row);
            }
            status.accept("Рейтинг сформирован: " + items.size() + " волонтёров.");
        } catch (SQLException ex) {
            status.accept("Ошибка: " + ex.getMessage());
        }
    }

    @FXML private void onExportCsv() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Экспорт рейтинга");
        fc.setInitialFileName("rating.csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        var f = fc.showSaveDialog(table.getScene().getWindow());
        if (f == null) return;
        try (FileWriter w = new FileWriter(f, StandardCharsets.UTF_8)) {
            w.write(CsvUtil.BOM);
            w.write(CsvUtil.line("rank", "fio", "hours", "events", "last_activity"));
            for (Row r : items) {
                w.write(CsvUtil.line(r.rank.get(), r.fio.get(),
                        r.hours.get(), r.events.get(), r.last.get()));
            }
            status.accept("Экспортировано: " + f.getName());
        } catch (IOException ex) {
            UiUtils.error("Не удалось экспортировать: " + ex.getMessage());
        }
    }
}
