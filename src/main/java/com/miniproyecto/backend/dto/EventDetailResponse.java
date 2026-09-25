package com.miniproyecto.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record EventDetailResponse(
        Long id,
        String name,
        String description,
        LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime time,
        String place,
        ClientResponse client,
        List<TaskResponse> tasks,
        long totalTasks,
        long doneTasks,
        int progress
) {
}
