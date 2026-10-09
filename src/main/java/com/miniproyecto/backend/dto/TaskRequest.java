package com.miniproyecto.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.miniproyecto.backend.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Si {@code estimatedHours} no viene, se calcula a partir del horario (o 1 h por defecto).
 * {@code status} solo se usa al editar; una gestión nueva siempre empieza PENDING.
 */
@Schema(description = "Gestión de un evento. startTime y endTime van juntos y el fin debe ser posterior al inicio "
        + "(error en el campo timeRangeValid). Al editar, dueDate y status solo cambian si vienen.")
public record TaskRequest(
        @Schema(example = "Reservar fotógrafo")
        @NotBlank(message = "El nombre de la gestión es obligatorio.") String name,
        @Schema(example = "Paquete de 6 horas")
        String description,
        @Schema(description = "Fecha límite; al crear, si no viene se usa la fecha actual", example = "2026-10-15")
        LocalDate dueDate,
        @Schema(type = "string", description = "Formato HH:mm", example = "09:00")
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @Schema(type = "string", description = "Formato HH:mm; posterior a startTime", example = "11:00")
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        @Schema(description = "Si no viene se calcula del horario, o 1 si no hay horario", example = "2")
        @Positive(message = "Las horas estimadas deben ser mayores que 0.") BigDecimal estimatedHours,
        @Schema(description = "Solo al editar; se ignora al crear", example = "PENDING")
        TaskStatus status
) {

    @JsonIgnore
    @AssertTrue(message = "La hora de fin debe ser posterior a la de inicio.")
    public boolean isTimeRangeValid() {
        if (startTime == null && endTime == null) {
            return true;
        }
        return startTime != null && endTime != null && endTime.isAfter(startTime);
    }
}
