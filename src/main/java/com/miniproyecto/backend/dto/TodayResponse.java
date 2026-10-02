package com.miniproyecto.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public record TodayResponse(
        @Schema(description = "Fecha actual del servidor", example = "2026-10-02") LocalDate today,
        @Schema(example = "1") long overdueCount,
        @Schema(example = "1") long todayCount,
        @Schema(example = "3") long upcomingCount,
        @Schema(description = "Ordenadas por fecha límite, luego por menor esfuerzo estimado") List<TodayTaskResponse> tasks
) {
}
