package com.sigo.programacion.domain.generador;

public record GeneradorExcepcion(
        GeneradorTrabajador trabajador,
        GeneradorPlaza plaza,
        Boolean permiteA,
        Boolean permiteB,
        Boolean permiteC,
        String motivo,
        Boolean activo
) {
    public GeneradorTrabajador getTrabajador() {
        return trabajador;
    }

    public GeneradorPlaza getPlaza() {
        return plaza;
    }

    public Boolean getPermiteA() {
        return permiteA;
    }

    public Boolean getPermiteB() {
        return permiteB;
    }

    public Boolean getPermiteC() {
        return permiteC;
    }

    public String getMotivo() {
        return motivo;
    }

    public Boolean getActivo() {
        return activo;
    }
}
