package com.sigo.programacion.application.port.in;

public interface AsignarSecuenciaUseCase {

    ListarSecuenciasUseCase.Secuencia asignar(Command command);

    record Command(
            Long agenteId,
            Long plazaId,
            String grupo
    ) {
    }
}
