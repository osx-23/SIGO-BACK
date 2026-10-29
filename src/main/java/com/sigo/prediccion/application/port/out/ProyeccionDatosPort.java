package com.sigo.prediccion.application.port.out;

import java.time.LocalDate;
import java.util.List;

public interface ProyeccionDatosPort {
    record TurnoOperativo(Long id, String codigo) {}
    record TurnoProgramado(LocalDate fecha, Long trabajadorId, String turnoCodigo) {}
    record AsistenciaRegistrada(LocalDate fecha, Long turnoId) {}

    List<TurnoOperativo> listarTurnos();
    List<TurnoProgramado> programacion(Long plazaId, LocalDate desde, LocalDate hasta);
    List<AsistenciaRegistrada> asistencias(Long plazaId, LocalDate desde, LocalDate hasta);
}
