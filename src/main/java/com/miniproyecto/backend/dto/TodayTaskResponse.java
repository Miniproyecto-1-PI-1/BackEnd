package com.miniproyecto.backend.dto;

import com.miniproyecto.backend.entity.TaskStatus;

import com.fasterxml.jackson.annotation.JsonFormat;

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
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        TodayCategory category,
        long daysFromToday,      // negativo = días de atraso
        Long eventId,
        String eventName,
        LocalDate eventDate,
        String clientName
) {
}
