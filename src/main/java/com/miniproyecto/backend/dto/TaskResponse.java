package com.miniproyecto.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.miniproyecto.backend.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record TaskResponse(
        @Schema(example = "101") Long id,
        @Schema(example = "Reservar fotógrafo") String name,
        @Schema(example = "Paquete de 6 horas") String description,
        @Schema(example = "2026-10-15") LocalDate dueDate,
        @Schema(type = "string", description = "Formato HH:mm", example = "09:00")
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @Schema(type = "string", description = "Formato HH:mm", example = "11:00")
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        @Schema(example = "2.00") BigDecimal estimatedHours,
        @Schema(example = "PENDING") TaskStatus status,
        @Schema(description = "Nota opcional; null si no tiene", example = "Esperando confirmación del salón")
        String note,
        @Schema(description = "Cuándo se ejecutó; null si no está hecha", example = "2026-10-14T16:30:00")
        LocalDateTime completedAt
) {
}
