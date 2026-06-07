package org.example.volonterhoursapp.model;

/**
 * Роль учётной записи. Определяет, какие разделы приложения доступны пользователю.
 * admin — полный доступ; curator — рабочие разделы; user — только просмотр.
 */
public enum Role {
    ADMIN("admin", "Администратор"),
    CURATOR("curator", "Куратор"),
    USER("user", "Пользователь");

    public final String id;
    public final String label;

    Role(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public static Role fromId(String id) {
        if (id == null) return USER;
        return switch (id.trim().toLowerCase()) {
            case "admin" -> ADMIN;
            case "curator" -> CURATOR;
            default -> USER;
        };
    }
}
