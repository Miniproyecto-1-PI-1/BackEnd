package com.miniproyecto.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record AvatarRequest(
        @NotBlank(message = "La imagen es obligatoria.") String image
) {
}
