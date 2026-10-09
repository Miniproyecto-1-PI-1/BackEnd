package com.miniproyecto.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Día donde la gestión sí cabe, con las horas libres que le quedan antes de llegar al límite. */
@Schema(description = "Día sugerido donde la gestión sí cabe")
public record SuggestedDate(
        @Schema(example = "2026-10-16") LocalDate date,
        @Schema(description = "Horas libres ese día antes de llegar al límite", example = "6") BigDecimal availableHours
) {
}
