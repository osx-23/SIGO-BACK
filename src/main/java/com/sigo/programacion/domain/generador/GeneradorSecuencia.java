package com.sigo.programacion.domain.generador;

import com.sigo.programacion.domain.ProgramacionGrupo;

public record GeneradorSecuencia(
        GeneradorTrabajador agente,
        GeneradorPlaza plaza,
        ProgramacionGrupo grupo,
        Integer orden
) {
    public GeneradorTrabajador getAgente() {
        return agente;
    }

    public GeneradorPlaza getPlaza() {
        return plaza;
    }

    public ProgramacionGrupo getGrupo() {
        return grupo;
    }

    public Integer getOrden() {
        return orden;
    }
}
