package com.sigo.asistencia.infrastructure.persistence.adapter;

import com.sigo.asistencia.application.port.in.MotivoAusenciaCatalogoUseCase;
import com.sigo.asistencia.application.port.out.MotivoAusenciaCatalogoPort;
import com.sigo.asistencia.infrastructure.persistence.repository.MotivoAusenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MotivoAusenciaCatalogoJpaAdapter
        implements MotivoAusenciaCatalogoPort {

    private final MotivoAusenciaRepository repository;

    @Override
    public List<MotivoAusenciaCatalogoUseCase.MotivoItem> listar() {
        return repository
                .findAll()
                .stream()
                .map(motivo ->
                        new MotivoAusenciaCatalogoUseCase.MotivoItem(
                                motivo.getId(),
                                motivo.getNombre()
                        )
                )
                .toList();
    }
}
