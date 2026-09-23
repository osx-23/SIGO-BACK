package com.sigo.relevo.infrastructure.persistence.adapter;

import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.relevo.application.port.in.ListarViasUseCase;
import com.sigo.relevo.application.port.out.ViaConsultaPort;
import com.sigo.relevo.infrastructure.persistence.repository.ViaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ViaConsultaJpaAdapter
        implements ViaConsultaPort {

    private final ViaRepository viaRepository;
    private final PlazaRepository plazaRepository;

    @Override
    public boolean existePlaza(Long plazaId) {
        return plazaRepository.existsById(plazaId);
    }

    @Override
    public List<ListarViasUseCase.Via> listarActivas(Long plazaId) {
        return viaRepository
                .findByPlazaIdAndActivaTrueOrderByOrdenAscNumeroAsc(
                        plazaId
                )
                .stream()
                .map(via ->
                        new ListarViasUseCase.Via(
                                via.getId(),
                                via.getPlaza().getId(),
                                via.getNumero(),
                                via.getNombre(),
                                via.getActiva(),
                                via.getOrden()
                        )
                )
                .toList();
    }
}
