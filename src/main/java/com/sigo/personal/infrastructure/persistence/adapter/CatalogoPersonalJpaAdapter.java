package com.sigo.personal.infrastructure.persistence.adapter;

import com.sigo.personal.application.port.in.CatalogoPersonalUseCase;
import com.sigo.personal.application.port.out.CatalogoPersonalQueryPort;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CatalogoPersonalJpaAdapter
        implements CatalogoPersonalQueryPort {

    private final PlazaRepository plazaRepository;
    private final TurnoRepository turnoRepository;

    @Override
    public List<CatalogoPersonalUseCase.PlazaItem> plazasActivas() {
        return plazaRepository
                .findByActivoTrueOrderByCodigoAsc()
                .stream()
                .map(plaza ->
                        new CatalogoPersonalUseCase.PlazaItem(
                                plaza.getId(),
                                plaza.getCodigo(),
                                plaza.getDescripcion(),
                                plaza.getActivo()
                        )
                )
                .toList();
    }

    @Override
    public List<CatalogoPersonalUseCase.TurnoItem> turnos() {
        return turnoRepository
                .findAll()
                .stream()
                .map(turno ->
                        new CatalogoPersonalUseCase.TurnoItem(
                                turno.getId(),
                                turno.getCodigo(),
                                turno.getDescripcion()
                        )
                )
                .toList();
    }
}
