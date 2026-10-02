package com.miniproyecto.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(
        @Schema(description = "JWT HS256 válido 8 horas; se envía como Authorization: Bearer <token>",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.firma")
        String token,
        UserResponse user
) {
}
