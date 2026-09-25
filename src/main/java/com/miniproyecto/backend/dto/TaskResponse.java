package com.miniproyecto.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.miniproyecto.backend.entity.TaskStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record TaskResponse(
        Long id,
        String name,
        String description,
        LocalDate dueDate,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        BigDecimal estimatedHours,
        TaskStatus status
) {
}
