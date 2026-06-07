package org.example.volonterhoursapp.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDateTime;

public class Participation {
    private final LongProperty id = new SimpleLongProperty();
    private final LongProperty volunteerId = new SimpleLongProperty();
    private final LongProperty eventId = new SimpleLongProperty();
    private final StringProperty volunteerName = new SimpleStringProperty();
    private final StringProperty eventTitle = new SimpleStringProperty();
    private final DoubleProperty hoursWorked = new SimpleDoubleProperty();
    private final BooleanProperty confirmed = new SimpleBooleanProperty();
    /** Внешний ключ на users — кто подтвердил (может быть null). */
    private final ObjectProperty<Long> confirmedByUserId = new SimpleObjectProperty<>();
    /** ФИО подтвердившего (из JOIN users) — только для отображения. */
    private final StringProperty confirmedByName = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> confirmedAt = new SimpleObjectProperty<>();

    public Participation() {}

    public long getId() { return id.get(); }
    public void setId(long v) { id.set(v); }
    public LongProperty idProperty() { return id; }

    public long getVolunteerId() { return volunteerId.get(); }
    public void setVolunteerId(long v) { volunteerId.set(v); }
    public LongProperty volunteerIdProperty() { return volunteerId; }

    public long getEventId() { return eventId.get(); }
    public void setEventId(long v) { eventId.set(v); }
    public LongProperty eventIdProperty() { return eventId; }

    public String getVolunteerName() { return volunteerName.get(); }
    public void setVolunteerName(String v) { volunteerName.set(v); }
    public StringProperty volunteerNameProperty() { return volunteerName; }

    public String getEventTitle() { return eventTitle.get(); }
    public void setEventTitle(String v) { eventTitle.set(v); }
    public StringProperty eventTitleProperty() { return eventTitle; }

    public double getHoursWorked() { return hoursWorked.get(); }
    public void setHoursWorked(double v) { hoursWorked.set(v); }
    public DoubleProperty hoursWorkedProperty() { return hoursWorked; }

    public boolean isConfirmed() { return confirmed.get(); }
    public void setConfirmed(boolean v) { confirmed.set(v); }
    public BooleanProperty confirmedProperty() { return confirmed; }

    public Long getConfirmedByUserId() { return confirmedByUserId.get(); }
    public void setConfirmedByUserId(Long v) { confirmedByUserId.set(v); }
    public ObjectProperty<Long> confirmedByUserIdProperty() { return confirmedByUserId; }

    public String getConfirmedByName() { return confirmedByName.get(); }
    public void setConfirmedByName(String v) { confirmedByName.set(v); }
    public StringProperty confirmedByNameProperty() { return confirmedByName; }

    public LocalDateTime getConfirmedAt() { return confirmedAt.get(); }
    public void setConfirmedAt(LocalDateTime v) { confirmedAt.set(v); }
    public ObjectProperty<LocalDateTime> confirmedAtProperty() { return confirmedAt; }
}
