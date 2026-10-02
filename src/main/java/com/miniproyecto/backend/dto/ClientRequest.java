package com.miniproyecto.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cliente del evento. Si ya existe uno del usuario con el mismo nombre (sin distinguir mayúsculas) "
        + "se reutiliza y se actualizan teléfono y correo si vienen; sin nombre, el evento queda sin cliente.")
public record ClientRequest(
        @Schema(example = "Andrea Muñoz") String name,
        @Schema(example = "3104567890") String phone,
        @Schema(example = "andrea.munoz@gmail.com") String email
) {
}
