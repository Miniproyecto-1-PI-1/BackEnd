package com.miniproyecto.backend.dto;

import java.time.LocalDate;
import java.util.List;

public record TodayResponse(
        LocalDate today,
        long overdueCount,
        long todayCount,
        long upcomingCount,
        List<TodayTaskResponse> tasks
) {
}