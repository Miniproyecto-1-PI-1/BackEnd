package com.miniproyecto.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @Schema(example = "valentina123")
        @NotBlank(message = "La contraseña actual es obligatoria.") String currentPassword,
        @Schema(description = "Debe ser distinta a la actual", example = "bodas&fiestas26")
        @NotBlank(message = "La nueva contraseña es obligatoria.")
        @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres.") String newPassword
) {
}
