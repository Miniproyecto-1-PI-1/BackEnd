package com.miniproyecto.backend.dto;

public record UserResponse(
        Long id,
        String name,
        String email,
        String avatar
) {
}
