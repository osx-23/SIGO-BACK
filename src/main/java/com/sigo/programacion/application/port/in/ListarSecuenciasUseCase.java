package com.sigo.programacion.application.port.in;

import java.util.List;

public interface ListarSecuenciasUseCase {

    List<Secuencia> listar(Long plazaId);

    record Secuencia(
            Long id,
            Long agenteId,
            Integer codigo,
            String nombre,
            Long plazaId,
            String plazaCodigo,
            String grupo,
            Integer orden
    ) {
    }
}
