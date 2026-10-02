package com.miniproyecto.backend.exception;

import java.util.Map;

/**
 * Cuerpo único para todas las respuestas de error de la API.
 * {@code errors} solo trae contenido en los 400 de validación (campo -> mensaje).
 */
public record ApiErrorResponse(
        int status,
        String title,
        String detail,
        Map<String, String> errors
) {
}
