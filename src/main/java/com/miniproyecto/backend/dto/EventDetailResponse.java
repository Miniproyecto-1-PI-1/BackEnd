package com.miniproyecto.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record EventDetailResponse(
        @Schema(example = "42") Long id,
        @Schema(example = "Boda Andrea y Julián") String name,
        @Schema(example = "Boda") String type,
        @Schema(example = "Ceremonia al aire libre y recepción para 120 invitados.") String description,
        @Schema(example = "2026-11-21") LocalDate date,
        @Schema(type = "string", description = "Formato HH:mm", example = "17:30")
        @JsonFormat(pattern = "HH:mm") LocalTime time,
        @Schema(example = "Hacienda El Paraíso, Cali") String place,
        @Schema(description = "null si el evento no tiene cliente") ClientResponse client,
        List<TaskResponse> tasks,
        @Schema(example = "4") long totalTasks,
        @Schema(example = "1") long doneTasks,
        @Schema(description = "Porcentaje de gestiones DONE, de 0 a 100", example = "25") int progress
) {
}
