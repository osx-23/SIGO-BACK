package com.sigo.asistencia.application.port.out;

public interface AsistenciaProgramacionCatalogoPort {

    String requirePlazaCodigo(Long plazaId);

    String requireTurnoCodigo(Long turnoId);
}
