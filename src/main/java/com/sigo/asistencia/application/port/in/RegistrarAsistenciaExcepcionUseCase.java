package com.sigo.asistencia.application.port.in;

public interface RegistrarAsistenciaExcepcionUseCase {

    ConsultarAsistenciasUseCase.Asistencia registrar(
            GestionarAsistenciaUseCase.Command command
    );
}
