package com.sigo.relevo.infrastructure.persistence.adapter;

import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.relevo.application.port.in.ConfigurarViasAviUseCase;
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
                .map(this::toVia)
                .toList();
    }

    @Override
    public List<ListarViasUseCase.Via> listarActivasVisiblesAvi(Long plazaId) {
        return viaRepository
                .findByPlazaIdAndActivaTrueAndAviVisibleTrueOrderByOrdenAscNumeroAsc(
                        plazaId
                )
                .stream()
                .map(this::toVia)
                .toList();
    }

    @Override
    public List<ConfigurarViasAviUseCase.ViaConfig> listarActivasParaConfigAvi(
            Long plazaId
    ) {
        return viaRepository
                .findByPlazaIdAndActivaTrueOrderByOrdenAscNumeroAsc(
                        plazaId
                )
                .stream()
                .map(via ->
                        new ConfigurarViasAviUseCase.ViaConfig(
                                via.getId(),
                                via.getPlaza().getId(),
                                via.getNumero(),
                                via.getNombre(),
                                via.getActiva(),
                                via.getOrden(),
                                Boolean.TRUE.equals(via.getAviVisible())
                        )
                )
                .toList();
    }

    @Override
    public void actualizarVisibilidadAvi(
            Long plazaId,
            Long viaId,
            boolean visible
    ) {
        var via = viaRepository
                .findByIdAndPlazaId(
                        viaId,
                        plazaId
                )
                .orElseThrow();

        via.setAviVisible(visible);
        viaRepository.save(via);
    }

    private ListarViasUseCase.Via toVia(
            com.sigo.relevo.infrastructure.persistence.entity.Via via
    ) {
        return new ListarViasUseCase.Via(
                via.getId(),
                via.getPlaza().getId(),
                via.getNumero(),
                via.getNombre(),
                via.getActiva(),
                via.getOrden()
        );
    }
}
