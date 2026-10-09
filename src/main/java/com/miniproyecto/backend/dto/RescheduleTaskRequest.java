package com.miniproyecto.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/** {@code estimatedHours} es opcional: se envía al resolver una sobrecarga reduciendo las horas. */
public record RescheduleTaskRequest(
        @NotNull(message = "La fecha límite es obligatoria.") LocalDate dueDate,
        @Positive(message = "Las horas estimadas deben ser mayores que 0.") BigDecimal estimatedHours
) {
}
