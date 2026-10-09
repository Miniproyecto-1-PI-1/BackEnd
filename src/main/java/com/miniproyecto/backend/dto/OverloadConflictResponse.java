package com.miniproyecto.backend.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Cuerpo del 409 cuando guardar una gestión dejaría un día por encima del límite diario. Sigue el formato de
 * error común ({@code status, title, detail, errors}) y agrega {@code overload} con las cifras del conflicto.
 */
@Schema(description = "Error 409 de sobrecarga diaria: formato de error común más el objeto overload")
public record OverloadConflictResponse(
        @Schema(example = "409") int status,
        @Schema(example = "Conflicto de sobrecarga") String title,
        @Schema(example = "Quedarías con 7h planificadas ese día (tu límite es 6h).") String detail,
        @Schema(description = "Vacío en este error", example = "{}") Map<String, String> errors,
        Overload overload
) {

    @Schema(name = "Overload", description = "Cifras del día que se pasaría del límite")
    public record Overload(
            @Schema(example = "2026-10-15") LocalDate date,
            @Schema(description = "Ya planificadas ese día, sin contar la gestión", example = "5")
            BigDecimal plannedHours,     // ya planificadas ese día, sin contar la gestión
            @Schema(description = "Horas de la gestión que se quiere ubicar", example = "2")
            BigDecimal taskHours,        // horas de la gestión que se quiere ubicar
            @Schema(description = "plannedHours + taskHours", example = "7")
            BigDecimal resultingHours,   // plannedHours + taskHours
            @Schema(description = "Límite diario del usuario", example = "6")
            int limitHours,
            @Schema(description = "resultingHours - limitHours", example = "1")
            BigDecimal exceedsBy,        // resultingHours - limitHours
            @Schema(description = "Lo que sí cabe ese día: limitHours - plannedHours (mínimo 0)", example = "1")
            BigDecimal availableHours,   // lo que sí cabe ese día: limitHours - plannedHours (mínimo 0)
            @ArraySchema(arraySchema = @Schema(description = "Días antes del evento donde la gestión sí cabe"))
            List<SuggestedDate> suggestedDates
    ) {
    }
}
