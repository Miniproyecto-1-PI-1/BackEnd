package com.miniproyecto.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record DailyLimitResponse(
        @Schema(description = "Límite diario de horas de gestión (entre 1 y 16)", example = "6") Integer dailyLimitHours
) {
}
