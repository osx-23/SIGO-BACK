package com.sigo.relevo.application.port.in;

import com.sigo.relevo.domain.EstadoRelevo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface GestionarRelevoUseCase {

    ConsultarRelevosUseCase.Relevo registrar(Command command);

    ConsultarRelevosUseCase.Relevo actualizar(
            Long id,
            Command command
    );

    record ChecklistItem(
            Long elementoId,
            EstadoRelevo estado,
            String detalle,
            Integer cantidad
    ) {
    }

    record ViaItem(
            Long viaId,
            EstadoRelevo estado,
            String detalle
    ) {
    }

    record Command(
            Long plazaId,
            Long turnoId,
            Long operadorId,
            LocalDate fecha,
            LocalTime hora,
            String observaciones,
            String resumen,
            List<ChecklistItem> checklist,
            List<ViaItem> vias
    ) {
    }
}
