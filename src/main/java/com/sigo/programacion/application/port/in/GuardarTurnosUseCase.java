package com.sigo.programacion.application.port.in;

import java.time.LocalDate;
import java.util.List;

public interface GuardarTurnosUseCase {

    List<ListarTurnosUseCase.Turno> guardar(Command command);

    record Command(
            Long plazaId,
            List<Item> programaciones
    ) {
    }

    record Item(
            Long trabajadorId,
            LocalDate fecha,
            String estado
    ) {
    }
}
