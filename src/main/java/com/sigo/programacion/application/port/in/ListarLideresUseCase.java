package com.sigo.programacion.application.port.in;

import java.time.LocalDate;
import java.util.List;

public interface ListarLideresUseCase {

    List<Lider> listar(Long plazaId);

    record Lider(
            Long id,
            Long agenteId,
            Integer agenteCodigo,
            String agenteNombre,
            Long controladorId,
            Integer controladorCodigo,
            String controladorNombre,
            Long plazaId,
            String plazaCodigo,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Boolean activo
    ) {
    }
}
