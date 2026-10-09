package com.miniproyecto.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClientResponse(
        @Schema(example = "9") Long id,
        @Schema(example = "Andrea Muñoz") String name,
        @Schema(example = "3104567890") String phone,
        @Schema(example = "andrea.munoz@gmail.com") String email
) {
}
