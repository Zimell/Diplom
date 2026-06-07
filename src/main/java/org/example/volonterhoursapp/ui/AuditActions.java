package org.example.volonterhoursapp.ui;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AuditActions {

    private static final Map<String, String> RU = new LinkedHashMap<>();

    static {
        RU.put("volunteer.create",       "Добавлен волонтёр");
        RU.put("volunteer.update",       "Изменён волонтёр");
        RU.put("volunteer.delete",       "Удалён волонтёр");

        RU.put("event.create",           "Создано мероприятие");
        RU.put("event.update",           "Изменено мероприятие");
        RU.put("event.delete",           "Удалено мероприятие");

        RU.put("participation.create",   "Добавлена запись участия");
        RU.put("participation.update",   "Изменена запись участия");
        RU.put("participation.confirm",  "Подтверждены часы");
        RU.put("participation.revoke",   "Снято подтверждение");
        RU.put("participation.delete",   "Удалена запись участия");

        RU.put("planner.enroll",         "Запись на мероприятие");
    }

    private AuditActions() {}

    public static String humanize(String code) {
        if (code == null) return "";
        String s = RU.get(code);
        return s != null ? s : code;
    }
}
