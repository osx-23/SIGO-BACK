package com.sigo.programacion.application.port.in;

import java.time.OffsetDateTime;
import java.util.List;

public interface AgenteProgramacionExcepcionUseCase {

    List<Excepcion> listarPorPlaza(Long plazaId);

    Excepcion obtener(Long trabajadorId, Long plazaId);

    Excepcion guardar(Command command);

    void desactivar(Long trabajadorId, Long plazaId);

    record Command(
            Long trabajadorId,
            Long plazaId,
            Boolean permiteA,
            Boolean permiteB,
            Boolean permiteC,
            String motivo,
            String color,
            Boolean activo
    ) {
    }

    record Excepcion(
            Long id,
            Long trabajadorId,
            Integer codigoTrabajador,
            String nombreTrabajador,
            Long plazaId,
            String plazaCodigo,
            Boolean permiteA,
            Boolean permiteB,
            Boolean permiteC,
            String motivo,
            String color,
            Boolean activo,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }
}
