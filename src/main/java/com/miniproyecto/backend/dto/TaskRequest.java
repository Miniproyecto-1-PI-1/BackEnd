package com.miniproyecto.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.miniproyecto.backend.entity.TaskStatus;
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
public record TaskRequest(
        @NotBlank(message = "El nombre de la gestión es obligatorio.") String name,
        String description,
        LocalDate dueDate,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        @Positive(message = "Las horas estimadas deben ser mayores que 0.") BigDecimal estimatedHours,
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
