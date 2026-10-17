package com.miniproyecto.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Avance de preparación de un evento. Las categorías no se solapan: {@code total} es la suma de
 * {@code done + pending + postponed + overdue}.
 */
public record EventProgressResponse(
        @Schema(example = "42") Long id,
        @Schema(example = "Boda Andrea y Julián") String name,
        @Schema(example = "2026-11-20") LocalDate date,
        @Schema(example = "8") long total,
        @Schema(description = "Gestiones ejecutadas", example = "3") long done,
        @Schema(description = "Pendientes con fecha de hoy o futura", example = "2") long pending,
        @Schema(description = "Pospuestas con fecha de hoy o futura", example = "1") long postponed,
        @Schema(description = "No ejecutadas con fecha anterior a hoy", example = "2") long overdue,
        @Schema(description = "done * 100 / total, redondeado; 0 si no hay gestiones", example = "38") int percent
) {
}
