package com.miniproyecto.backend.dto;

public record ClientResponse(
        Long id,
        String name,
        String phone,
        String email
) {
}
