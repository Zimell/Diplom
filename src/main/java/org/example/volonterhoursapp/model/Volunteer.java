package org.example.volonterhoursapp.model;

import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;

public class Volunteer {
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty fio = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> birthDate = new SimpleObjectProperty<>();
    private final StringProperty phone = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> registeredAt = new SimpleObjectProperty<>();

    public Volunteer() {}

    public Volunteer(long id, String fio, LocalDate birthDate, String phone, String email, LocalDate registeredAt) {
        this.id.set(id);
        this.fio.set(fio);
        this.birthDate.set(birthDate);
        this.phone.set(phone);
        this.email.set(email);
        this.registeredAt.set(registeredAt);
    }

    public long getId() { return id.get(); }
    public void setId(long v) { id.set(v); }
    public LongProperty idProperty() { return id; }

    public String getFio() { return fio.get(); }
    public void setFio(String v) { fio.set(v); }
    public StringProperty fioProperty() { return fio; }

    public LocalDate getBirthDate() { return birthDate.get(); }
    public void setBirthDate(LocalDate v) { birthDate.set(v); }
    public ObjectProperty<LocalDate> birthDateProperty() { return birthDate; }

    public String getPhone() { return phone.get(); }
    public void setPhone(String v) { phone.set(v); }
    public StringProperty phoneProperty() { return phone; }

    public String getEmail() { return email.get(); }
    public void setEmail(String v) { email.set(v); }
    public StringProperty emailProperty() { return email; }

    public LocalDate getRegisteredAt() { return registeredAt.get(); }
    public void setRegisteredAt(LocalDate v) { registeredAt.set(v); }
    public ObjectProperty<LocalDate> registeredAtProperty() { return registeredAt; }

    @Override
    public String toString() {
        return getFio() + " (#" + getId() + ")";
    }
}
