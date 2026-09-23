package com.sigo.asistencia.application.port.in;

public interface ObtenerProgramadosAsistenciaUseCase {

    int obtenerProgramados(Long plazaId, Long turnoId);
}
