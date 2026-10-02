package com.miniproyecto.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres.") String name,
        @NotBlank(message = "El correo es obligatorio.") @Email(message = "Ingresa un correo válido.") String email,
        String currentPassword
) {
}
