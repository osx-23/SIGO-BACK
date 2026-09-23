package com.sigo.asistencia.application.port.out;

import com.sigo.asistencia.application.port.in.GestionarAsistenciaUseCase;

public interface AsistenciaExcepcionPort {

    Long registrar(
            GestionarAsistenciaUseCase.Command command
    );
}
