package com.miniproyecto.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/** Reemplaza los datos del evento; las gestiones se editan por sus propios endpoints. */
public record UpdateEventRequest(
        @NotBlank(message = "El nombre es obligatorio.") String name,
        @Size(max = 50, message = "El tipo no puede superar 50 caracteres.") String type,
        @NotNull(message = "La fecha es obligatoria.") LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime time,
        @NotBlank(message = "El lugar es obligatorio.") String place,
        String description,
        @Valid ClientRequest client
) {
}
