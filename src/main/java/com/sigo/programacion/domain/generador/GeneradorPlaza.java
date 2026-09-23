package com.sigo.programacion.domain.generador;

public record GeneradorPlaza(
        Long id,
        String codigo
) {
    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }
}
