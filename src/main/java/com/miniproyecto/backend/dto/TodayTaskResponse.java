package com.miniproyecto.backend.dto;

import com.miniproyecto.backend.entity.TaskStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record TodayTaskResponse(
        Long id,
        String name,
        String description,
        TaskStatus status,
        BigDecimal estimatedHours,
        LocalDate dueDate,
        LocalTime startTime,
        LocalTime endTime,
        TodayCategory category,
        long daysFromToday,      // negativo = días de atraso
        Long eventId,
        String eventName,
        String clientName
) {
}
