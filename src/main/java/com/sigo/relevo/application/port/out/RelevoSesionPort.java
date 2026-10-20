package com.sigo.relevo.application.port.out;

import com.sigo.relevo.application.port.in.RelevoSesionUseCase;
import java.util.Optional;

public interface RelevoSesionPort {
    Optional<RelevoSesionUseCase.Sesion> enProceso(Long operadorId);
    Optional<RelevoSesionUseCase.Sesion> obtener(Long id);
    RelevoSesionUseCase.Sesion crear(Long operadorId, Long plazaId, Long turnoId);
    RelevoSesionUseCase.Sesion guardarBorrador(Long id, String borrador);
    RelevoSesionUseCase.Sesion finalizar(Long id, Long relevoId);
    RelevoSesionUseCase.Sesion cancelar(Long id);
}
