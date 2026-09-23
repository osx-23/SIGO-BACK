package com.sigo.security.domain;

public enum RolSeguridad {
    SUPERVISOR,
    CONTROLADOR,
    OPERADOR;

    public static RolSeguridad from(String value) {
        return RolSeguridad.valueOf(value.trim().toUpperCase());
    }
}
