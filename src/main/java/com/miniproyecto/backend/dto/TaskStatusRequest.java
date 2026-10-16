package com.miniproyecto.backend.dto;

import com.miniproyecto.backend.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Cambio de estado de una gestión (hecha, pospuesta o de nuevo pendiente) con nota opcional.
 * {@code dueDate} solo se usa al posponer; sin ella, la gestión pasa a la próxima fecha libre.
 */
public record TaskStatusRequest(
        @Schema(example = "POSTPONED", description = "PENDING, DONE o POSTPONED")
        @NotNull(message = "El estado es obligatorio.") TaskStatus status,
        @Schema(example = "Esperando confirmación del salón")
        @Size(max = 500, message = "La nota no puede superar 500 caracteres.") String note,
        @Schema(example = "2026-10-20", description = "Solo al posponer; opcional")
        LocalDate dueDate
) {
}
