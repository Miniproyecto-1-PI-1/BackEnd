package com.miniproyecto.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CreateEventRequest(
        @Schema(example = "Boda Andrea y Julián")
        @NotBlank(message = "El nombre es obligatorio.") String name,
        @Schema(description = "Texto libre, p. ej. Boda, Social, Corporativo, Cumpleaños u Otro", example = "Boda")
        @Size(max = 50, message = "El tipo no puede superar 50 caracteres.") String type,
        @Schema(example = "2026-11-21")
        @NotNull(message = "La fecha es obligatoria.") LocalDate date,
        @Schema(type = "string", description = "Formato HH:mm; si no viene se usa 00:00", example = "17:30")
        @JsonFormat(pattern = "HH:mm") LocalTime time,
        @Schema(example = "Hacienda El Paraíso, Cali")
        @NotBlank(message = "El lugar es obligatorio.") String place,
        @Schema(example = "Ceremonia al aire libre y recepción para 120 invitados.")
        String description,
        @Valid ClientRequest client,
        @Schema(description = "Gestiones iniciales; todas empiezan en PENDING")
        @Valid List<@Valid TaskRequest> tasks
) {
}
