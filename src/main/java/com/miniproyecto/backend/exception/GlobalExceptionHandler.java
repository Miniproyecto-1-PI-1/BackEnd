package com.miniproyecto.backend.exception;

import com.miniproyecto.backend.dto.OverloadConflictResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Solicitud inválida", "Revisa los campos marcados", errors);
    }

    @ExceptionHandler(FieldErrorException.class)
    public ResponseEntity<ApiErrorResponse> handleFieldErrors(FieldErrorException ex) {
        return build(HttpStatus.BAD_REQUEST, "Solicitud inválida", "Revisa los campos marcados", ex.getErrors());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Solicitud inválida", "El cuerpo de la petición no es un JSON válido o tiene un formato incorrecto", Map.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "Solicitud inválida", "El parámetro '" + ex.getName() + "' tiene un valor inválido", Map.of());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage(), Map.of());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        return build(HttpStatus.UNAUTHORIZED, "No autenticado", ex.getMessage(), Map.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, "Conflicto", ex.getMessage(), Map.of());
    }

    @ExceptionHandler(OverloadConflictException.class)
    public ResponseEntity<OverloadConflictResponse> handleOverload(OverloadConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new OverloadConflictResponse(
                HttpStatus.CONFLICT.value(),
                "Conflicto de sobrecarga",
                ex.getMessage(),
                "overload_conflict",
                ex.getDate(),
                ex.getPlannedHours(),
                ex.getTaskHours(),
                ex.getResultingHours(),
                ex.getLimitHours(),
                ex.getExceedsBy(),
                ex.getAvailableHours(),
                ex.getSuggestedDates()
        ));
    }

    @ExceptionHandler(OverloadConflictException.class)
    public ResponseEntity<OverloadConflictResponse> handleOverload(OverloadConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new OverloadConflictResponse(
                HttpStatus.CONFLICT.value(),
                "Conflicto de sobrecarga",
                ex.getMessage(),
                "overload_conflict",
                ex.getDate(),
                ex.getPlannedHours(),
                ex.getTaskHours(),
                ex.getResultingHours(),
                ex.getLimitHours(),
                ex.getSuggestedDates()
        ));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Recurso no encontrado", "La ruta solicitada no existe", Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido", "El método " + ex.getMethod() + " no está soportado en esta ruta", Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "Ha ocurrido un error interno", Map.of());
    }

    private static ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String title,
            String detail,
            Map<String, String> errors
    ) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(status.value(), title, detail, errors));
    }
}
