package com.miniproyecto.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record EventSummaryResponse(
        @Schema(example = "42") Long id,
        @Schema(example = "Boda Andrea y Julián") String name,
        @Schema(example = "Boda") String type,
        @Schema(example = "2026-11-21") LocalDate date,
        @Schema(example = "Andrea Muñoz") String clientName,
        @Schema(example = "4") long totalTasks,
        @Schema(example = "1") long doneTasks,
        @Schema(description = "Porcentaje de gestiones DONE, de 0 a 100", example = "25") int progress
) {
}
