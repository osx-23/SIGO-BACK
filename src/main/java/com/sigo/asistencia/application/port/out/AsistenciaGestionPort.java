package com.sigo.asistencia.application.port.out;

import com.sigo.asistencia.application.port.in.GestionarAsistenciaUseCase;

public interface AsistenciaGestionPort {

    Long registrar(
            GestionarAsistenciaUseCase.Command command
    );

    Long actualizar(
            Long id,
            GestionarAsistenciaUseCase.Command command
    );
}
