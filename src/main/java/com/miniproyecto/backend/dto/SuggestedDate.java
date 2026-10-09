package com.miniproyecto.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Día donde la gestión sí cabe, con las horas libres que le quedan antes de llegar al límite. */
public record SuggestedDate(LocalDate date, BigDecimal availableHours) {
}
