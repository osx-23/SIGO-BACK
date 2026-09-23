package com.sigo.programacion.infrastructure.persistence.adapter;

import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.programacion.application.port.in.ListarSecuenciasUseCase;
import com.sigo.programacion.application.port.out.SecuenciaGestionPort;
import com.sigo.programacion.infrastructure.persistence.entity.GrupoProgramacion;
import com.sigo.programacion.infrastructure.persistence.entity.ProgramacionSecuenciaAgente;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionSecuenciaAgenteRepository;
import com.sigo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SecuenciaGestionJpaAdapter
        implements SecuenciaGestionPort {

    private final ProgramacionSecuenciaAgenteRepository secuenciaRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final PlazaRepository plazaRepository;

    @Override
    public List<ListarSecuenciasUseCase.Secuencia> listar(Long plazaId) {
        return secuenciaRepository
                .findActivasByPlazaId(plazaId)
                .stream()
                .map(this::toData)
                .toList();
    }

    @Override
    public ListarSecuenciasUseCase.Secuencia asignar(
            Long agenteId,
            Long plazaId,
            String grupo,
            Long actualizadoPorId
    ) {
        GrupoProgramacion grupoDestino = parseGrupo(grupo);

        Plaza plaza = plazaRepository
                .findById(plazaId)
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .orElseThrow(() -> bad("Plaza no válida"));

        Trabajador agente = trabajadorRepository
                .findById(agenteId)
                .filter(t -> Boolean.TRUE.equals(t.getActivo()))
                .orElseThrow(() -> bad("Agente no encontrado"));

        validarAgentePertenecePlaza(agente, plazaId);

        Trabajador actual = trabajadorRepository
                .findById(actualizadoPorId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Supervisor actual no encontrado"
                        )
                );

        ProgramacionSecuenciaAgente registro = secuenciaRepository
                .findByAgenteId(agenteId)
                .orElseGet(() -> {
                    ProgramacionSecuenciaAgente nuevo =
                            new ProgramacionSecuenciaAgente();
                    nuevo.setAgente(agente);
                    nuevo.setPlaza(plaza);
                    return nuevo;
                });

        GrupoProgramacion grupoAnterior = registro.getGrupo();

        if (grupoAnterior == grupoDestino) {
            return toData(registro);
        }

        registro.setGrupo(null);
        registro.setOrden(null);
        registro.setActualizadoPor(actual);
        registro = secuenciaRepository.save(registro);
        secuenciaRepository.flush();

        if (grupoAnterior != null) {
            normalizarGrupo(plazaId, grupoAnterior, actual);
        }

        normalizarGrupo(plazaId, grupoDestino, actual);

        int siguienteOrden = secuenciaRepository
                .findActivasByPlazaIdAndGrupo(
                        plazaId,
                        grupoDestino
                )
                .size() + 1;

        registro.setGrupo(grupoDestino);
        registro.setOrden(siguienteOrden);
        registro.setActualizadoPor(actual);

        registro = secuenciaRepository.save(registro);
        secuenciaRepository.flush();

        return toData(registro);
    }

    private void normalizarGrupo(
            Long plazaId,
            GrupoProgramacion grupo,
            Trabajador actual
    ) {
        secuenciaRepository.deleteInactivasByPlazaId(plazaId);
        secuenciaRepository.flush();

        List<ProgramacionSecuenciaAgente> registros =
                secuenciaRepository.findActivasByPlazaIdAndGrupo(
                        plazaId,
                        grupo
                );

        if (registros.isEmpty()) {
            return;
        }

        for (ProgramacionSecuenciaAgente registro : registros) {
            registro.setGrupo(null);
            registro.setOrden(null);
            registro.setActualizadoPor(actual);
            secuenciaRepository.save(registro);
        }

        secuenciaRepository.flush();

        int orden = 1;

        for (ProgramacionSecuenciaAgente registro : registros) {
            registro.setGrupo(grupo);
            registro.setOrden(orden++);
            registro.setActualizadoPor(actual);
            secuenciaRepository.save(registro);
        }

        secuenciaRepository.flush();
    }

    private void validarAgentePertenecePlaza(
            Trabajador agente,
            Long plazaId
    ) {
        if (agente.getPlaza() == null) {
            throw bad("El agente no tiene plaza asignada");
        }

        if (!Objects.equals(
                agente.getPlaza().getId(),
                plazaId
        )) {
            throw bad(
                    "El agente no pertenece a la plaza seleccionada"
            );
        }

        Set<Long> agentesValidos = trabajadorRepository
                .findAgentesByPlaza(plazaId)
                .stream()
                .map(Trabajador::getId)
                .collect(Collectors.toSet());

        if (!agentesValidos.contains(agente.getId())) {
            throw bad(
                    "El trabajador seleccionado no es un agente activo de la plaza"
            );
        }
    }

    private GrupoProgramacion parseGrupo(String grupo) {
        try {
            return GrupoProgramacion.valueOf(grupo);
        } catch (Exception exception) {
            throw bad("Grupo de programación no válido");
        }
    }

    private ListarSecuenciasUseCase.Secuencia toData(
            ProgramacionSecuenciaAgente secuencia
    ) {
        return new ListarSecuenciasUseCase.Secuencia(
                secuencia.getId(),
                secuencia.getAgente().getId(),
                secuencia.getAgente().getCodigo(),
                secuencia.getAgente().getNombreCompleto(),
                secuencia.getPlaza().getId(),
                secuencia.getPlaza().getCodigo(),
                secuencia.getGrupo() == null
                        ? null
                        : secuencia.getGrupo().name(),
                secuencia.getOrden()
        );
    }

    private BusinessException bad(String message) {
        return new BusinessException(message);
    }
}
