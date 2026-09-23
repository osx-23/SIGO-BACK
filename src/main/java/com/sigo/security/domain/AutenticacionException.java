package com.sigo.security.domain;

public class AutenticacionException extends RuntimeException {

    private final Tipo tipo;

    public AutenticacionException(
            Tipo tipo,
            String message
    ) {
        super(message);
        this.tipo = tipo;
    }

    public Tipo tipo() {
        return tipo;
    }

    public enum Tipo {
        UNAUTHORIZED,
        BAD_REQUEST,
        FORBIDDEN,
        NOT_FOUND
    }
}
