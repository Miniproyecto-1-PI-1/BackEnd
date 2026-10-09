package com.miniproyecto.backend.dto;

import com.miniproyecto.backend.entity.TaskStatus;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class TodayTaskResponseJsonTest {

    @Test
    void timesAreSerializedAsHHmmSoTheClientCanSendThemBackToPutTask() {
        TodayTaskResponse task = new TodayTaskResponse(
                1L, "Reservar salón", null, TaskStatus.PENDING, new BigDecimal("4.00"), LocalDate.of(2026, 10, 9),
                LocalTime.of(9, 0), LocalTime.of(13, 0), TodayCategory.UPCOMING, 1, 5L, "Boda",
                LocalDate.of(2026, 11, 7), null);

        String json = JsonMapper.builder().build().writeValueAsString(task);

        assertThat(json).contains("\"startTime\":\"09:00\"").contains("\"endTime\":\"13:00\"");
    }
}
