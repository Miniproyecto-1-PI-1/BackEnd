package com.miniproyecto.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(
        @Schema(description = "Contraseña actual, para confirmar", example = "valentina123")
        @NotBlank(message = "La contraseña es obligatoria.") String password
) {
}
