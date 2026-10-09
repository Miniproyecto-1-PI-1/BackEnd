package com.miniproyecto.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AvatarRequest(
        @Schema(description = "Data URL de una imagen JPG, PNG o WebP en base64, de máximo 400 000 caracteres",
                example = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAgGBgcGBQgH")
        @NotBlank(message = "La imagen es obligatoria.") String image
) {
}
