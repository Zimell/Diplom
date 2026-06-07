package org.example.volonterhoursapp.controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import org.example.volonterhoursapp.dao.AuditDAO;
import org.example.volonterhoursapp.dao.VolunteerDAO;
import org.example.volonterhoursapp.model.Volunteer;
import org.example.volonterhoursapp.ui.CsvUtil;
import org.example.volonterhoursapp.ui.CuratorSession;
import org.example.volonterhoursapp.ui.UiUtils;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Consumer;

public class VolunteersController implements Refreshable, StatusAware {

    @FXML private TextField searchField;
    @FXML private TableView<Volunteer> table;
    @FXML private TableColumn<Volunteer, Number> colId;
    @FXML private TableColumn<Volunteer, String> colFio;
    @FXML private TableColumn<Volunteer, String> colBirth;
    @FXML private TableColumn<Volunteer, String> colPhone;
    @FXML private TableColumn<Volunteer, String> colEmail;
    @FXML private TableColumn<Volunteer, String> colRegistered;

    @FXML private TextField fioField;
    @FXML private DatePicker birthPicker;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private DatePicker regPicker;

    private final VolunteerDAO dao = new VolunteerDAO();
    private final AuditDAO audit = new AuditDAO();
    private final ObservableList<Volunteer> items = FXCollections.observableArrayList();
    private Consumer<String> status = s -> {};

    @FXML
    private void initialize() {
        colId.setCellValueFactory(d -> d.getValue().idProperty());
        colFio.setCellValueFactory(d -> d.getValue().fioProperty());
        colBirth.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getBirthDate() == null ? "" : d.getValue().getBirthDate().toString()));
        colPhone.setCellValueFactory(d -> d.getValue().phoneProperty());
        colEmail.setCellValueFactory(d -> d.getValue().emailProperty());
        colRegistered.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getRegisteredAt() == null ? "" : d.getValue().getRegisteredAt().toString()));

        table.setItems(items);
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> populateForm(b));
    }

    @Override public void setStatusConsumer(Consumer<String> status) { this.status = status; }

    @Override
    public void refresh() {
        try {
            items.setAll(dao.findAll());
            status.accept("Загружено волонтёров: " + items.size());
        } catch (SQLException ex) {
            status.accept("Ошибка: " + ex.getMessage());
        }
    }

    @FXML private void onSearch() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim();
        if (q.isEmpty()) { refresh(); return; }
        try {
            items.setAll(dao.search(q));
            status.accept("Найдено: " + items.size());
        } catch (SQLException ex) {
            status.accept("Ошибка поиска: " + ex.getMessage());
        }
    }

    @FXML private void onResetSearch() {
        searchField.clear();
        refresh();
    }

    @FXML private void onNew() {
        table.getSelectionModel().clearSelection();
        fioField.clear();
        birthPicker.setValue(null);
        phoneField.clear();
        emailField.clear();
        regPicker.setValue(null);
        fioField.requestFocus();
    }

    @FXML private void onSave() {
        String fio = fioField.getText() == null ? "" : fioField.getText().trim();
        if (fio.isEmpty()) { UiUtils.error("ФИО обязательно."); return; }

        Volunteer current = table.getSelectionModel().getSelectedItem();
        try {
            if (current == null) {
                Volunteer v = new Volunteer();
                v.setFio(fio);
                v.setBirthDate(birthPicker.getValue());
                v.setPhone(text(phoneField));
                v.setEmail(text(emailField));
                v.setRegisteredAt(regPicker.getValue());
                dao.insert(v);
                audit.log(CuratorSession.getUserId(), "volunteer.create", v.getFio() + " (#" + v.getId() + ")");
                status.accept("Создан волонтёр #" + v.getId());
            } else {
                current.setFio(fio);
                current.setBirthDate(birthPicker.getValue());
                current.setPhone(text(phoneField));
                current.setEmail(text(emailField));
                current.setRegisteredAt(regPicker.getValue());
                dao.update(current);
                audit.log(CuratorSession.getUserId(), "volunteer.update", current.getFio() + " (#" + current.getId() + ")");
                status.accept("Сохранены изменения #" + current.getId());
            }
            refresh();
        } catch (SQLException ex) {
            UiUtils.error("Не удалось сохранить: " + ex.getMessage());
        }
    }

    @FXML private void onDelete() {
        Volunteer v = table.getSelectionModel().getSelectedItem();
        if (v == null) { status.accept("Выберите волонтёра."); return; }
        if (!UiUtils.confirm("Удалить волонтёра «" + v.getFio() + "»? Все его участия тоже будут удалены."))
            return;
        try {
            dao.delete(v.getId());
            audit.log(CuratorSession.getUserId(), "volunteer.delete", v.getFio() + " (#" + v.getId() + ")");
            refresh();
            onNew();
        } catch (SQLException ex) {
            UiUtils.error("Не удалось удалить: " + ex.getMessage());
        }
    }

    @FXML private void onExportCsv() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Экспорт волонтёров в CSV");
        fc.setInitialFileName("volunteers.csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        var f = fc.showSaveDialog(table.getScene().getWindow());
        if (f == null) return;
        try (FileWriter w = new FileWriter(f, StandardCharsets.UTF_8)) {
            w.write(CsvUtil.BOM);
            w.write(CsvUtil.line("id", "fio", "birth_date", "phone", "email", "registered_at"));
            for (Volunteer v : items) {
                w.write(CsvUtil.line(v.getId(), v.getFio(), v.getBirthDate(),
                        v.getPhone(), v.getEmail(), v.getRegisteredAt()));
            }
            status.accept("Экспортировано в " + f.getName());
        } catch (IOException ex) {
            UiUtils.error("Не удалось экспортировать: " + ex.getMessage());
        }
    }

    @FXML private void onImportCsv() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Импорт волонтёров из CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        var f = fc.showOpenDialog(table.getScene().getWindow());
        if (f == null) return;

        int created = 0, updated = 0, skipped = 0;
        try (BufferedReader r = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
            String header = r.readLine();
            if (header == null) { UiUtils.error("Файл пуст."); return; }
            char sep = CsvUtil.detectSeparator(header);

            String row;
            int lineNo = 1;
            while ((row = r.readLine()) != null) {
                lineNo++;
                if (row.isBlank()) continue;
                List<String> c = CsvUtil.parseLine(row, sep);
                // Колонки: id;fio;birth_date;phone;email;registered_at — обязателен только fio.
                String fio = col(c, 1).trim();
                if (fio.isEmpty()) { skipped++; continue; }

                Volunteer v = new Volunteer();
                v.setFio(fio);
                v.setBirthDate(parseDate(col(c, 2)));
                v.setPhone(blankToNull(col(c, 3)));
                v.setEmail(blankToNull(col(c, 4)));
                LocalDate reg = parseDate(col(c, 5));
                v.setRegisteredAt(reg != null ? reg : LocalDate.now());

                Long id = parseId(col(c, 0));
                try {
                    if (id != null && dao.findById(id) != null) {
                        v.setId(id);
                        dao.update(v);
                        updated++;
                    } else {
                        dao.insert(v);
                        created++;
                    }
                } catch (SQLException ex) {
                    skipped++;
                    status.accept("Строка " + lineNo + ": " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            UiUtils.error("Не удалось прочитать файл: " + ex.getMessage());
            return;
        }

        audit.log(CuratorSession.getUserId(), "volunteer.import",
                "создано: " + created + ", обновлено: " + updated + ", пропущено: " + skipped);
        refresh();
        UiUtils.info("Импорт завершён.\nСоздано: " + created
                + "\nОбновлено: " + updated
                + "\nПропущено: " + skipped);
    }

    private static String col(List<String> row, int i) {
        return i < row.size() ? row.get(i) : "";
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private static Long parseId(String s) {
        s = s == null ? "" : s.trim();
        if (s.isEmpty()) return null;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
    }

    private static LocalDate parseDate(String s) {
        s = s == null ? "" : s.trim();
        if (s.isEmpty()) return null;
        try { return LocalDate.parse(s); } catch (DateTimeParseException e) { return null; }
    }

    private void populateForm(Volunteer v) {
        if (v == null) return;
        fioField.setText(v.getFio());
        birthPicker.setValue(v.getBirthDate());
        phoneField.setText(v.getPhone());
        emailField.setText(v.getEmail());
        regPicker.setValue(v.getRegisteredAt());
    }

    private static String text(TextField f) {
        String s = f.getText();
        return s == null ? null : s.trim();
    }
}
