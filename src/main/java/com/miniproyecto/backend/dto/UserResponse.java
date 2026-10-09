package com.miniproyecto.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Valentina Vélez") String name,
        @Schema(example = "valentina@eventosvv.co") String email,
        @Schema(description = "Foto como data URL, o null si no tiene", example = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAgGBgcGBQgH")
        String avatar,
        @Schema(description = "Límite diario de horas de gestión (entre 1 y 16)", example = "6") Integer dailyLimitHours
) {
}
