package com.miniproyecto.backend.dto;

public record AuthResponse(
        String token,
        UserResponse user
) {
}
