package com.sigo.programacion.application.port.in;

import java.util.List;

public interface GuardarOrdenSecuenciaUseCase {

    void guardar(Command command);

    record Command(
            Long plazaId,
            String grupo,
            List<Item> agentes
    ) {
    }

    record Item(
            Long agenteId,
            Integer orden
    ) {
    }
}
