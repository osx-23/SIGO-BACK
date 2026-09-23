package com.sigo.programacion.domain.generador;

public record GeneradorTrabajador(
        Long id,
        Integer codigo,
        String nombreCompleto,
        GeneradorPlaza plaza
) {
    public Long getId() {
        return id;
    }

    public Integer getCodigo() {
        return codigo;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public GeneradorPlaza getPlaza() {
        return plaza;
    }
}
