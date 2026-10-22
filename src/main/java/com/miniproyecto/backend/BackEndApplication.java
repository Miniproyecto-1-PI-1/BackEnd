package com.miniproyecto.backend;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackEndApplication {

    public static void main(String[] args) {
        // El servidor (Render) corre en UTC: fijamos la zona de la app para que LocalDate.now() sea el "hoy" de Colombia
        TimeZone.setDefault(TimeZone.getTimeZone(System.getenv().getOrDefault("APP_TIMEZONE", "America/Bogota")));
        SpringApplication.run(BackEndApplication.class, args);
    }

}
