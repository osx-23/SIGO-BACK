package com.sigo.programacion.infrastructure.persistence.adapter;

import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.programacion.application.port.in.GuardarOrdenSecuenciaUseCase;
import com.sigo.programacion.application.port.out.SecuenciaOrdenPort;
import com.sigo.programacion.infrastructure.persistence.entity.GrupoProgramacion;
import com.sigo.programacion.infrastructure.persistence.entity.ProgramacionSecuenciaAgente;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionSecuenciaAgenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SecuenciaOrdenJpaAdapter
        implements SecuenciaOrdenPort {

    private final ProgramacionSecuenciaAgenteRepository secuenciaRepository;
    private final PlazaRepository plazaRepository;
    private final TrabajadorRepository trabajadorRepository;

    @Override
    public boolean existePlazaActiva(Long plazaId) {
        return plazaRepository
                .findById(plazaId)
                .filter(plaza -> Boolean.TRUE.equals(plaza.getActivo()))
                .isPresent();
    }

    @Override
    public void eliminarSecuenciasInactivas(Long plazaId) {
        secuenciaRepository.deleteInactivasByPlazaId(plazaId);
        secuenciaRepository.flush();
    }

    @Override
    public List<Long> obtenerAgentesActivos(
            Long plazaId,
            String grupo
    ) {
        GrupoProgramacion grupoProgramacion =
                GrupoProgramacion.valueOf(grupo);

        return secuenciaRepository
                .findActivasByPlazaIdAndGrupo(
                        plazaId,
                        grupoProgramacion
                )
                .stream()
                .map(secuencia ->
                        secuencia.getAgente().getId()
                )
                .toList();
    }

    @Override
    public void reordenar(
            Long plazaId,
            String grupo,
            List<GuardarOrdenSecuenciaUseCase.Item> agentes,
            Long actualizadoPorId
    ) {
        GrupoProgramacion grupoProgramacion =
                GrupoProgramacion.valueOf(grupo);

        Trabajador actualizadoPor =
                trabajadorRepository
                        .findById(actualizadoPorId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Supervisor actual no encontrado"
                                )
                        );

        List<ProgramacionSecuenciaAgente> actuales =
                secuenciaRepository
                        .findActivasByPlazaIdAndGrupo(
                                plazaId,
                                grupoProgramacion
                        );

        Map<Long, ProgramacionSecuenciaAgente> porAgente =
                actuales
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        secuencia ->
                                                secuencia.getAgente().getId(),
                                        secuencia -> secuencia
                                )
                        );

        for (ProgramacionSecuenciaAgente registro : actuales) {
            registro.setGrupo(null);
            registro.setOrden(null);
            registro.setActualizadoPor(actualizadoPor);
            secuenciaRepository.save(registro);
        }

        secuenciaRepository.flush();

        for (GuardarOrdenSecuenciaUseCase.Item item :
                agentes.stream()
                        .sorted(
                                Comparator.comparing(
                                        GuardarOrdenSecuenciaUseCase.Item::orden
                                )
                        )
                        .toList()) {

            ProgramacionSecuenciaAgente registro =
                    porAgente.get(item.agenteId());

            registro.setGrupo(grupoProgramacion);
            registro.setOrden(item.orden());
            registro.setActualizadoPor(actualizadoPor);

            secuenciaRepository.save(registro);
        }

        secuenciaRepository.flush();
    }
}
