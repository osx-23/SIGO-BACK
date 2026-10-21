package com.sigo.prediccion.domain;

public class PrediccionNoDisponibleException extends RuntimeException {
    public PrediccionNoDisponibleException(String message) {
        super(message);
    }

    public PrediccionNoDisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
