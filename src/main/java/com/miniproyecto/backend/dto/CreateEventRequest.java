package com.miniproyecto.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CreateEventRequest(
        @NotBlank String name,
        @NotNull LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime time,
        @NotBlank String place,
        String description,
        @Valid ClientRequest client,
        @Valid List<@Valid TaskRequest> tasks
) {
}
