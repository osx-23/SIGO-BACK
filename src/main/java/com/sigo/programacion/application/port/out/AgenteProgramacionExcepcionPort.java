package com.sigo.programacion.application.port.out;

import com.sigo.programacion.application.port.in.AgenteProgramacionExcepcionUseCase;

import java.util.List;

public interface AgenteProgramacionExcepcionPort {

    List<AgenteProgramacionExcepcionUseCase.Excepcion> listarPorPlaza(
            Long plazaId
    );

    AgenteProgramacionExcepcionUseCase.Excepcion obtener(
            Long trabajadorId,
            Long plazaId
    );

    AgenteProgramacionExcepcionUseCase.Excepcion guardar(
            AgenteProgramacionExcepcionUseCase.Command command
    );

    void desactivar(Long trabajadorId, Long plazaId);

    boolean existePlazaActiva(Long plazaId);

    boolean trabajadorActivoPertenecePlaza(
            Long trabajadorId,
            Long plazaId
    );
}
