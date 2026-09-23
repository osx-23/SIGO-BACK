package com.sigo.programacion.application.port.in;

import java.time.LocalDate;

public interface AsignarLiderUseCase {

    ListarLideresUseCase.Lider asignar(Command command);

    record Command(
            Long agenteId,
            Long controladorId,
            Long plazaId,
            LocalDate fechaInicio
    ) {
    }
}
