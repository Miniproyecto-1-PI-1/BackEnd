package com.miniproyecto.backend.exception;

import com.miniproyecto.backend.dto.SuggestedDate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class OverloadConflictException extends RuntimeException {

    private final LocalDate date;
    private final BigDecimal plannedHours;
    private final BigDecimal taskHours;
    private final BigDecimal resultingHours;
    private final int limitHours;
    private final List<SuggestedDate> suggestedDates;

    public OverloadConflictException(
            LocalDate date,
            BigDecimal plannedHours,
            BigDecimal taskHours,
            BigDecimal resultingHours,
            int limitHours,
            List<SuggestedDate> suggestedDates
    ) {
        super("Ese día superaría tu límite diario de horas.");
        this.date = date;
        this.plannedHours = plannedHours;
        this.taskHours = taskHours;
        this.resultingHours = resultingHours;
        this.limitHours = limitHours;
        this.suggestedDates = suggestedDates;
    }

    public BigDecimal getExceedsBy() {
        return resultingHours.subtract(BigDecimal.valueOf(limitHours));
    }

    public BigDecimal getAvailableHours() {
        return BigDecimal.valueOf(limitHours).subtract(plannedHours).max(BigDecimal.ZERO);
    }

    public LocalDate getDate() { return date; }
    public BigDecimal getPlannedHours() { return plannedHours; }
    public BigDecimal getTaskHours() { return taskHours; }
    public BigDecimal getResultingHours() { return resultingHours; }
    public int getLimitHours() { return limitHours; }
    public List<SuggestedDate> getSuggestedDates() { return suggestedDates; }
}
