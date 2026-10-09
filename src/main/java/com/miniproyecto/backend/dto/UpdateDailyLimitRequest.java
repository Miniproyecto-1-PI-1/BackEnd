package com.miniproyecto.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateDailyLimitRequest(
        @NotNull(message = "El límite diario es obligatorio.")
        @Min(value = 1, message = "El valor debe estar entre 1 y 16 horas.")
        @Max(value = 16, message = "El valor debe estar entre 1 y 16 horas.") Integer dailyLimitHours
) {
}
