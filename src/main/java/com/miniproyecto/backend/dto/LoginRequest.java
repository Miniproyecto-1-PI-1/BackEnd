package com.miniproyecto.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "No distingue mayúsculas", example = "valentina@eventosvv.co")
        @NotBlank(message = "El correo es obligatorio.") String email,
        @Schema(example = "valentina123")
        @NotBlank(message = "La contraseña es obligatoria.") String password
) {
}
