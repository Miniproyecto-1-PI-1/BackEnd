package com.miniproyecto.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Cuerpo del 409 cuando reprogramar dejaría un día por encima del límite diario. */
public record OverloadConflictResponse(
        int status,
        String title,
        String detail,
        String code,
        LocalDate date,
        BigDecimal plannedHours,     // ya planificadas ese día, sin contar la gestión
        BigDecimal taskHours,        // horas de la gestión que se quiere ubicar
        BigDecimal resultingHours,   // plannedHours + taskHours
        int limitHours,
        BigDecimal exceedsBy,        // resultingHours - limitHours
        BigDecimal availableHours,   // lo que sí cabe ese día: limitHours - plannedHours (mínimo 0)
        List<LocalDate> suggestedDates
) {
}
