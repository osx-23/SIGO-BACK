package com.sigo.programacion.infrastructure.persistence.adapter;

import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.RolSistema;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.programacion.application.port.in.ListarLideresUseCase;
import com.sigo.programacion.application.port.out.LiderGestionPort;
import com.sigo.programacion.infrastructure.persistence.entity.AgenteControladorLider;
import com.sigo.programacion.infrastructure.persistence.repository.AgenteControladorLiderRepository;
import com.sigo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LiderGestionJpaAdapter
        implements LiderGestionPort {

    private final AgenteControladorLiderRepository liderRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final PlazaRepository plazaRepository;

    @Override
    public List<ListarLideresUseCase.Lider> listar(Long plazaId) {
        return liderRepository
                .findLideresActivosPorPlaza(plazaId)
                .stream()
                .map(this::toData)
                .toList();
    }

    @Override
    public ListarLideresUseCase.Lider asignar(
            Long agenteId,
            Long controladorId,
            Long plazaId,
            LocalDate fechaInicio,
            Long asignadoPorId
    ) {
        Plaza plaza = plazaRepository
                .findById(plazaId)
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .orElseThrow(() -> bad("Plaza no válida"));

        Trabajador agente = trabajadorRepository
                .findById(agenteId)
                .filter(t -> Boolean.TRUE.equals(t.getActivo()))
                .orElseThrow(() -> bad("Agente no encontrado"));

        Trabajador controlador = trabajadorRepository
                .findById(controladorId)
                .filter(t -> Boolean.TRUE.equals(t.getActivo()))
                .orElseThrow(() -> bad("Controlador no encontrado"));

        Trabajador asignadoPor = trabajadorRepository
                .findById(asignadoPorId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Supervisor actual no encontrado"
                        )
                );

        Set<Long> agentesValidos = trabajadorRepository
                .findAgentesByPlaza(plazaId)
                .stream()
                .map(Trabajador::getId)
                .collect(Collectors.toSet());

        if (!agentesValidos.contains(agenteId)) {
            throw bad(
                    "El trabajador seleccionado no es un agente activo de la plaza"
            );
        }

        if (controlador.getRolSistema() != RolSistema.CONTROLADOR) {
            throw bad("El líder debe tener rol CONTROLADOR");
        }

        if (controlador.getPlaza() == null
                || !Objects.equals(
                        controlador.getPlaza().getId(),
                        plazaId
                )) {
            throw bad("El controlador no pertenece a la plaza");
        }

        liderRepository
                .findByAgenteIdAndActivoTrue(agenteId)
                .ifPresent(anterior -> {
                    anterior.setActivo(false);

                    LocalDate fin = fechaInicio.minusDays(1);
                    if (fin.isBefore(anterior.getFechaInicio())) {
                        fin = anterior.getFechaInicio();
                    }

                    anterior.setFechaFin(fin);
                    liderRepository.save(anterior);
                });

        AgenteControladorLider nuevo =
                new AgenteControladorLider();

        nuevo.setAgente(agente);
        nuevo.setControlador(controlador);
        nuevo.setPlaza(plaza);
        nuevo.setFechaInicio(fechaInicio);
        nuevo.setActivo(true);
        nuevo.setAsignadoPor(asignadoPor);

        return toData(liderRepository.save(nuevo));
    }

    private ListarLideresUseCase.Lider toData(
            AgenteControladorLider lider
    ) {
        return new ListarLideresUseCase.Lider(
                lider.getId(),
                lider.getAgente().getId(),
                lider.getAgente().getCodigo(),
                lider.getAgente().getNombreCompleto(),
                lider.getControlador().getId(),
                lider.getControlador().getCodigo(),
                lider.getControlador().getNombreCompleto(),
                lider.getPlaza().getId(),
                lider.getPlaza().getCodigo(),
                lider.getFechaInicio(),
                lider.getFechaFin(),
                lider.getActivo()
        );
    }

    private BusinessException bad(String message) {
        return new BusinessException(message);
    }
}
