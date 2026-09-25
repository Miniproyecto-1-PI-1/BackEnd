package com.miniproyecto.backend.security;

public final class CurrentUser {

    private static final long USER_ID = 1L;

    private CurrentUser() {
    }

    /** Luego se reemplaza por el userId del JWT. */
    public static long id() {
        return USER_ID;
    }
}
