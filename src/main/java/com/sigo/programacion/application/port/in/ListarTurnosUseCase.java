package com.sigo.programacion.application.port.in;

import java.time.LocalDate;
import java.util.List;

public interface ListarTurnosUseCase {

    List<Turno> listar(Long plazaId, int anio, int mes);

    record Turno(
            Long programacionId,
            Long trabajadorId,
            Integer codigoTrabajador,
            String nombreTrabajador,
            Long plazaId,
            String plazaCodigo,
            LocalDate fecha,
            String estado
    ) {
    }
}
