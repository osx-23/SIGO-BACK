package com.sigo.relevo.application.port.in;

import java.util.List;

public interface ListarViasUseCase {

    List<Via> listarPorPlaza(Long plazaId);

    List<Via> listarVisiblesAvi(Long plazaId);

    record Via(
            Long id,
            Long plazaId,
            Integer numero,
            String nombre,
            Boolean activa,
            Integer orden
    ) {
    }
}
