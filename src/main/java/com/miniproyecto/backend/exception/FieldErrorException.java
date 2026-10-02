package com.miniproyecto.backend.exception;

import java.util.Map;

/** Error de negocio asociado a un campo concreto; se responde como un 400 de validación. */
public class FieldErrorException extends RuntimeException {

    private final Map<String, String> errors;

    public FieldErrorException(String field, String message) {
        super(message);
        this.errors = Map.of(field, message);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
