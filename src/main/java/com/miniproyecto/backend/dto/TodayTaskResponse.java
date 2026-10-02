package com.miniproyecto.backend.dto;

import com.miniproyecto.backend.entity.TaskStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TodayTaskResponse(
        Long id,
        String name,
        TaskStatus status,
        BigDecimal estimatedHours,
        LocalDate dueDate,
        TodayCategory category,
        long daysFromToday,      // negativo = días de atraso
        Long eventId,
        String eventName,
        String clientName
) {
}