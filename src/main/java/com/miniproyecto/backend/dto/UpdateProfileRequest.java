package com.miniproyecto.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Schema(example = "Valentina Vélez")
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres.") String name,
        @Schema(description = "Se guarda en minúsculas y sin espacios", example = "valentina.velez@eventosvv.co")
        @NotBlank(message = "El correo es obligatorio.") @Email(message = "Ingresa un correo válido.") String email,
        @Schema(description = "Contraseña actual; obligatoria solo si cambia el correo", example = "valentina123")
        String currentPassword
) {
}
