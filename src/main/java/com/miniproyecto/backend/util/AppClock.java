package com.miniproyecto.backend.util;

import java.time.LocalDate;
import java.time.ZoneId;

/** "Hoy" según la zona horaria de la app (por defecto Colombia), no la del servidor, que suele estar en UTC. */
public final class AppClock {

    private static final ZoneId ZONE = ZoneId.of(System.getenv().getOrDefault("APP_TIMEZONE", "America/Bogota"));

    private AppClock() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
