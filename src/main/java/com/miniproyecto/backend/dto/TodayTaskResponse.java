package com.miniproyecto.backend.dto;

import com.miniproyecto.backend.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record TodayTaskResponse(
        @Schema(example = "95") Long id,
        @Schema(example = "Llamar al DJ") String name,
        @Schema(example = "Confirmar lista de canciones") String description,
        @Schema(example = "PENDING") TaskStatus status,
        @Schema(example = "1.00") BigDecimal estimatedHours,
        @Schema(example = "2026-10-02") LocalDate dueDate,
        @Schema(type = "string", description = "Formato HH:mm:ss", example = "10:00:00") LocalTime startTime,
        @Schema(type = "string", description = "Formato HH:mm:ss", example = "11:00:00") LocalTime endTime,
        @Schema(example = "TODAY") TodayCategory category,
        @Schema(description = "Días hasta la fecha límite; negativo = días de atraso", example = "0")
        long daysFromToday,      // negativo = días de atraso
        @Schema(example = "42") Long eventId,
        @Schema(example = "Boda Andrea y Julián") String eventName,
        @Schema(example = "Andrea Muñoz") String clientName
) {
}
