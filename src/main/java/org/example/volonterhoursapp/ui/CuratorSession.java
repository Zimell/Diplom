package org.example.volonterhoursapp.ui;

import org.example.volonterhoursapp.model.Role;
import org.example.volonterhoursapp.model.User;

/**
 * Текущая сессия. После входа хранит залогиненного пользователя и его роль.
 * Id пользователя ({@link #getUserId()}) подставляется в подтверждения участий
 * и в журнал действий (внешние ключи confirmed_by_user_id и actor_user_id).
 */
public final class CuratorSession {

    private static User user;

    private CuratorSession() {}

    public static void login(User u) {
        user = u;
    }

    public static void logout() {
        user = null;
    }

    public static User getUser() {
        return user;
    }

    public static Role getRole() {
        return user == null ? null : user.getRole();
    }

    /** Id вошедшего пользователя (для записи в журнал и подтверждения участий) или null. */
    public static Long getUserId() {
        return user == null ? null : user.getId();
    }

    public static boolean isAdmin()   { return getRole() == Role.ADMIN; }
    public static boolean isCurator() { return getRole() == Role.CURATOR; }
}
