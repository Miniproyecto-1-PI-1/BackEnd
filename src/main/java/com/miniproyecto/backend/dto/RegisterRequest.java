package com.miniproyecto.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El nombre es obligatorio.") String name,
        @NotBlank(message = "El correo es obligatorio.") @Email(message = "Ingresa un correo válido.") String email,
        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres.") String password
) {
}
