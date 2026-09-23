package com.sigo.programacion.infrastructure.persistence.adapter;

import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.programacion.application.port.in.GuardarTurnosUseCase;
import com.sigo.programacion.application.port.in.ListarTurnosUseCase;
import com.sigo.programacion.application.port.out.TurnoGestionPort;
import com.sigo.programacion.infrastructure.persistence.entity.EstadoProgramacion;
import com.sigo.programacion.infrastructure.persistence.entity.ProgramacionTurno;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionTurnoRepository;
import com.sigo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TurnoGestionJpaAdapter
        implements TurnoGestionPort {

    private final ProgramacionTurnoRepository programacionRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final PlazaRepository plazaRepository;

    @Override
    public List<ListarTurnosUseCase.Turno> listar(
            Long plazaId,
            LocalDate desde,
            LocalDate hasta
    ) {
        return programacionRepository
                .findMesResumen(
                        plazaId,
                        desde,
                        hasta
                )
                .stream()
                .map(this::toData)
                .toList();
    }

    @Override
    public List<ListarTurnosUseCase.Turno> guardar(
            Long plazaId,
            List<GuardarTurnosUseCase.Item> programaciones,
            Long supervisorId
    ) {
        Plaza plaza = plazaRepository
                .findById(plazaId)
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .orElseThrow(() -> bad("Plaza no válida"));

        Trabajador supervisor = trabajadorRepository
                .findById(supervisorId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Supervisor actual no encontrado"
                        )
                );

        Map<Long, Trabajador> agentesPorId = trabajadorRepository
                .findAgentesByPlaza(plazaId)
                .stream()
                .collect(Collectors.toMap(
                        Trabajador::getId,
                        trabajador -> trabajador
                ));

        Set<Long> trabajadorIds = new HashSet<>();
        LocalDate fechaMin = null;
        LocalDate fechaMax = null;

        for (GuardarTurnosUseCase.Item item : programaciones) {
            if (item == null
                    || item.trabajadorId() == null
                    || item.fecha() == null
                    || item.estado() == null) {
                throw bad("La programación contiene datos incompletos");
            }

            if (!agentesPorId.containsKey(item.trabajadorId())) {
                throw bad(
                        "El trabajador "
                                + item.trabajadorId()
                                + " no es un agente activo de la plaza"
                );
            }

            parseEstado(item.estado());
            trabajadorIds.add(item.trabajadorId());

            if (fechaMin == null || item.fecha().isBefore(fechaMin)) {
                fechaMin = item.fecha();
            }
            if (fechaMax == null || item.fecha().isAfter(fechaMax)) {
                fechaMax = item.fecha();
            }
        }

        List<ProgramacionTurno> existentes =
                programacionRepository.findParaGuardadoMasivo(
                        plazaId,
                        trabajadorIds,
                        fechaMin,
                        fechaMax
                );

        Map<Long, Map<LocalDate, ProgramacionTurno>> existentesPorTrabajador =
                new HashMap<>();

        for (ProgramacionTurno programacion : existentes) {
            existentesPorTrabajador
                    .computeIfAbsent(
                            programacion.getTrabajador().getId(),
                            ignored -> new HashMap<>()
                    )
                    .put(programacion.getFecha(), programacion);
        }

        List<ProgramacionTurno> paraGuardar =
                new ArrayList<>(programaciones.size());

        for (GuardarTurnosUseCase.Item item : programaciones) {
            Trabajador trabajador =
                    agentesPorId.get(item.trabajadorId());

            Map<LocalDate, ProgramacionTurno> porFecha =
                    existentesPorTrabajador.computeIfAbsent(
                            item.trabajadorId(),
                            ignored -> new HashMap<>()
                    );

            ProgramacionTurno programacion =
                    porFecha.get(item.fecha());

            if (programacion == null) {
                programacion = new ProgramacionTurno();
                programacion.setTrabajador(trabajador);
                programacion.setPlaza(plaza);
                programacion.setFecha(item.fecha());
                programacion.setCreadoPor(supervisor);

                porFecha.put(item.fecha(), programacion);
                paraGuardar.add(programacion);
            } else if (!paraGuardar.contains(programacion)) {
                paraGuardar.add(programacion);
            }

            programacion.setPlaza(plaza);
            programacion.setEstado(parseEstado(item.estado()));
            programacion.setActualizadoPor(supervisor);
        }

        return programacionRepository
                .saveAllAndFlush(paraGuardar)
                .stream()
                .map(this::toData)
                .toList();
    }

    private EstadoProgramacion parseEstado(String estado) {
        try {
            return EstadoProgramacion.valueOf(estado);
        } catch (Exception exception) {
            throw bad("Estado de programación no válido");
        }
    }

    private ListarTurnosUseCase.Turno toData(
            ProgramacionTurno programacion
    ) {
        return new ListarTurnosUseCase.Turno(
                programacion.getId(),
                programacion.getTrabajador().getId(),
                programacion.getTrabajador().getCodigo(),
                programacion.getTrabajador().getNombreCompleto(),
                programacion.getPlaza().getId(),
                programacion.getPlaza().getCodigo(),
                programacion.getFecha(),
                programacion.getEstado().name()
        );
    }

    private ListarTurnosUseCase.Turno toData(
            ProgramacionTurnoRepository.TurnoResumen programacion
    ) {
        return new ListarTurnosUseCase.Turno(
                programacion.getProgramacionId(),
                programacion.getTrabajadorId(),
                programacion.getCodigoTrabajador(),
                programacion.getNombreTrabajador(),
                programacion.getPlazaId(),
                programacion.getPlazaCodigo(),
                programacion.getFecha(),
                programacion.getEstado().name()
        );
    }

    private BusinessException bad(String message) {
        return new BusinessException(message);
    }
}
