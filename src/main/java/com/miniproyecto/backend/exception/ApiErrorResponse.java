package com.miniproyecto.backend.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Cuerpo único para todas las respuestas de error de la API.
 * {@code errors} solo trae contenido en los 400 de validación (campo -> mensaje).
 */
public record ApiErrorResponse(
        @Schema(example = "400") int status,
        @Schema(example = "Solicitud inválida") String title,
        @Schema(example = "Revisa los campos marcados") String detail,
        @Schema(description = "Campo -> mensaje; vacío salvo en los 400 de validación",
                example = "{\"name\": \"El nombre es obligatorio.\"}")
        Map<String, String> errors
) {
}
