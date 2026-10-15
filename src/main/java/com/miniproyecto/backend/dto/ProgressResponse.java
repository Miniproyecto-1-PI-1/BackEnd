package com.miniproyecto.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Progreso general del organizador y desglose por evento (vista /progreso). */
public record ProgressResponse(
        @Schema(example = "17") long total,
        @Schema(example = "6") long done,
        @Schema(example = "5") long pending,
        @Schema(example = "3") long postponed,
        @Schema(example = "3") long overdue,
        @Schema(example = "35") int percent,
        List<EventProgressResponse> events
) {
}
