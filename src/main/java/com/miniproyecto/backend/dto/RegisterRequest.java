package com.miniproyecto.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(description = "Nombre visible; se guarda sin espacios al inicio ni al final", example = "Camila Torres")
        @NotBlank(message = "El nombre es obligatorio.") String name,
        @Schema(description = "Se guarda en minúsculas y sin espacios", example = "camila.torres@gmail.com")
        @NotBlank(message = "El correo es obligatorio.") @Email(message = "Ingresa un correo válido.") String email,
        @Schema(example = "fiesta2026")
        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres.") String password
) {
}
