package com.miniproyecto.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Cuerpo del 409 cuando guardar una gestión dejaría un día por encima del límite diario. Sigue el formato de
 * error común ({@code status, title, detail, errors}) y agrega {@code overload} con las cifras del conflicto.
 */
public record OverloadConflictResponse(
        int status,
        String title,
        String detail,
        Map<String, String> errors,
        Overload overload
) {

    public record Overload(
            LocalDate date,
            BigDecimal plannedHours,     // ya planificadas ese día, sin contar la gestión
            BigDecimal taskHours,        // horas de la gestión que se quiere ubicar
            BigDecimal resultingHours,   // plannedHours + taskHours
            int limitHours,
            BigDecimal exceedsBy,        // resultingHours - limitHours
            BigDecimal availableHours,   // lo que sí cabe ese día: limitHours - plannedHours (mínimo 0)
            List<SuggestedDate> suggestedDates
    ) {
    }
}
