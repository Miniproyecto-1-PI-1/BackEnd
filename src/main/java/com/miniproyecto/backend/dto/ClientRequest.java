package com.miniproyecto.backend.dto;

public record ClientRequest(
        String name,
        String phone,
        String email
) {
}
