package org.example.volonterhoursapp.model;

import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDateTime;

public class Event {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty title = new SimpleStringProperty();
    private final ObjectProperty<LocalDateTime> datetime = new SimpleObjectProperty<>();
    private final StringProperty location = new SimpleStringProperty();
    private final StringProperty type = new SimpleStringProperty();
    /** Внешний ключ на справочник organizers. */
    private final LongProperty organizerId = new SimpleLongProperty();
    /** Название организатора (из JOIN organizers) — только для отображения. */
    private final StringProperty organizerName = new SimpleStringProperty();

    public Event() {}

    public long getId() { return id.get(); }
    public void setId(long v) { id.set(v); }
    public LongProperty idProperty() { return id; }

    public String getTitle() { return title.get(); }
    public void setTitle(String v) { title.set(v); }
    public StringProperty titleProperty() { return title; }

    public LocalDateTime getDatetime() { return datetime.get(); }
    public void setDatetime(LocalDateTime v) { datetime.set(v); }
    public ObjectProperty<LocalDateTime> datetimeProperty() { return datetime; }

    public String getLocation() { return location.get(); }
    public void setLocation(String v) { location.set(v); }
    public StringProperty locationProperty() { return location; }

    public String getType() { return type.get(); }
    public void setType(String v) { type.set(v); }
    public StringProperty typeProperty() { return type; }

    public long getOrganizerId() { return organizerId.get(); }
    public void setOrganizerId(long v) { organizerId.set(v); }
    public LongProperty organizerIdProperty() { return organizerId; }

    public String getOrganizerName() { return organizerName.get(); }
    public void setOrganizerName(String v) { organizerName.set(v); }
    public StringProperty organizerNameProperty() { return organizerName; }

    @Override
    public String toString() {
        return getTitle() + " (#" + getId() + ")";
    }
}
