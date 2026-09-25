package com.miniproyecto.backend.dto;

import java.time.LocalDate;

public record EventSummaryResponse(
        Long id,
        String name,
        String type,
        LocalDate date,
        String clientName,
        long totalTasks,
        long doneTasks,
        int progress
) {
}
