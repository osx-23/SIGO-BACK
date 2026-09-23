package com.sigo.asistencia.infrastructure.persistence.adapter;

import com.sigo.asistencia.application.port.out.AsistenciaProgramacionCatalogoPort;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.TurnoRepository;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AsistenciaProgramacionCatalogoJpaAdapter
        implements AsistenciaProgramacionCatalogoPort {

    private final PlazaRepository plazaRepository;
    private final TurnoRepository turnoRepository;

    @Override
    public String requirePlazaCodigo(Long plazaId) {
        return plazaRepository
                .findById(plazaId)
                .map(plaza -> plaza.getCodigo())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Plaza no encontrada"
                        )
                );
    }

    @Override
    public String requireTurnoCodigo(Long turnoId) {
        return turnoRepository
                .findById(turnoId)
                .map(turno -> turno.getCodigo())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Turno no encontrado"
                        )
                );
    }
}
