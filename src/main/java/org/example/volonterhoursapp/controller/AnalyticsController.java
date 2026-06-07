package org.example.volonterhoursapp.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import org.example.volonterhoursapp.dao.ParticipationDAO;
import org.example.volonterhoursapp.dao.VolunteerDAO;
import org.example.volonterhoursapp.model.Volunteer;

import java.sql.SQLException;
import java.util.Map;
import java.util.function.Consumer;

public class AnalyticsController implements Refreshable, StatusAware {

    @FXML private ComboBox<Volunteer> volunteerCombo;
    @FXML private LineChart<String, Number> chart;

    private final VolunteerDAO volunteerDAO = new VolunteerDAO();
    private final ParticipationDAO participationDAO = new ParticipationDAO();
    private Consumer<String> status = s -> {};

    @FXML
    private void initialize() {
        volunteerCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Volunteer v) { return v == null ? "" : v.getFio() + " (#" + v.getId() + ")"; }
            @Override public Volunteer fromString(String s) { return null; }
        });
        volunteerCombo.valueProperty().addListener((o, a, b) -> rebuild(b));
    }

    @Override public void setStatusConsumer(Consumer<String> status) { this.status = status; }

    @Override
    public void refresh() {
        try {
            volunteerCombo.getItems().setAll(volunteerDAO.findAll());
            if (!volunteerCombo.getItems().isEmpty() && volunteerCombo.getValue() == null) {
                volunteerCombo.setValue(volunteerCombo.getItems().get(0));
            } else {
                rebuild(volunteerCombo.getValue());
            }
        } catch (SQLException ex) {
            status.accept("Ошибка: " + ex.getMessage());
        }
    }

    @FXML private void onRefresh() {
        rebuild(volunteerCombo.getValue());
    }

    private void rebuild(Volunteer v) {
        chart.getData().clear();
        if (v == null) return;
        try {
            Map<String, Double> data = participationDAO.hoursByMonth(v.getId());
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(v.getFio());
            for (Map.Entry<String, Double> e : data.entrySet()) {
                series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
            }
            chart.setData(FXCollections.observableArrayList(series));
            status.accept(data.isEmpty()
                    ? "Нет подтверждённых часов у выбранного волонтёра."
                    : "Месяцев с активностью: " + data.size());
        } catch (SQLException ex) {
            status.accept("Ошибка: " + ex.getMessage());
        }
    }
}
