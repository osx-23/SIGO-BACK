package com.sigo.relevo.application.port.in;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;

public interface RelevoSesionUseCase {
    Sesion iniciar(Long plazaId, Long turnoId);
    Sesion actual();
    Sesion guardarBorrador(Long id, String borrador);
    Sesion finalizar(Long id, GestionarRelevoUseCase.Command command);
    Sesion cancelar(Long id);
    List<Sesion> historial(LocalDate inicio, LocalDate fin);

    record Sesion(
            Long id, Long operadorId, Long plazaId, Long turnoId,
            String estado, OffsetDateTime iniciadoEn, OffsetDateTime finalizadoEn,
            Long relevoId, String borrador
    ) {}
}
