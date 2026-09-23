package com.sigo.programacion.domain.generador;

import com.sigo.programacion.domain.ProgramacionEstado;

import java.time.LocalDate;

public record GeneradorTurno(
        GeneradorTrabajador trabajador,
        GeneradorPlaza plaza,
        LocalDate fecha,
        ProgramacionEstado estado
) {
    public GeneradorTrabajador getTrabajador() {
        return trabajador;
    }

    public GeneradorPlaza getPlaza() {
        return plaza;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public ProgramacionEstado getEstado() {
        return estado;
    }
}
