package com.sigo.programacion.infrastructure.persistence.adapter;

import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.programacion.application.port.in.DistribucionUseCase;
import com.sigo.programacion.application.port.out.DistribucionGestionPort;
import com.sigo.programacion.infrastructure.persistence.entity.DistribucionPersonal;
import com.sigo.programacion.infrastructure.persistence.entity.ProgramacionTurno;
import com.sigo.programacion.infrastructure.persistence.entity.ProgramacionUbicacion;
import com.sigo.programacion.infrastructure.persistence.repository.DistribucionPersonalRepository;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionTurnoRepository;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionUbicacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@Component
@RequiredArgsConstructor
public class DistribucionGestionJpaAdapter
        implements DistribucionGestionPort {

    private final DistribucionPersonalRepository distribucionRepository;
    private final ProgramacionTurnoRepository programacionRepository;
    private final ProgramacionUbicacionRepository ubicacionRepository;
    private final TrabajadorRepository trabajadorRepository;

    @Override
    public List<DistribucionUseCase.Distribucion> listar(
            Long plazaId,
            LocalDate desde,
            LocalDate hasta
    ) {
        return distribucionRepository
                .findMes(plazaId, desde, hasta)
                .stream()
                .map(this::toData)
                .toList();
    }

    @Override
    public List<DistribucionUseCase.Distribucion> guardar(
            Long plazaId,
            List<DistribucionUseCase.Item> distribuciones,
            Long usuarioId
    ) {
        Trabajador actual = trabajadorRepository
                .findById(usuarioId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Usuario actual no encontrado"
                        )
                );

        List<DistribucionPersonal> guardados =
                new ArrayList<>();

        for (DistribucionUseCase.Item item : distribuciones) {
            if (item == null
                    || item.programacionTurnoId() == null
                    || item.ubicacionId() == null) {
                throw bad(
                        "La distribución contiene datos incompletos"
                );
            }

            ProgramacionTurno programacion =
                    programacionRepository
                            .findById(item.programacionTurnoId())
                            .orElseThrow(() ->
                                    bad(
                                            "Programación no encontrada: "
                                                    + item.programacionTurnoId()
                                    )
                            );

            if (!Objects.equals(
                    programacion.getPlaza().getId(),
                    plazaId
            )) {
                throw bad(
                        "La programación no pertenece a la plaza"
                );
            }

            if (!programacion.getEstado().esOperativo()) {
                throw bad(
                        "Solo los estados A, B o C pueden tener caseta"
                );
            }

            ProgramacionUbicacion ubicacion =
                    ubicacionRepository
                            .findById(item.ubicacionId())
                            .filter(x ->
                                    Boolean.TRUE.equals(x.getActivo())
                            )
                            .orElseThrow(() ->
                                    bad("Ubicación no válida")
                            );

            if (!Objects.equals(
                    ubicacion.getPlaza().getId(),
                    plazaId
            )) {
                throw bad(
                        "La ubicación no pertenece a la plaza"
                );
            }

            DistribucionPersonal distribucion =
                    distribucionRepository
                            .findByProgramacionTurnoId(
                                    programacion.getId()
                            )
                            .orElseGet(() -> {
                                DistribucionPersonal nueva =
                                        new DistribucionPersonal();
                                nueva.setProgramacionTurno(programacion);
                                nueva.setAsignadoPor(actual);
                                return nueva;
                            });

            distribucion.setUbicacion(ubicacion);
            distribucion.setObservacion(item.observacion());
            distribucion.setActualizadoPor(actual);

            guardados.add(
                    distribucionRepository.save(distribucion)
            );
        }

        distribucionRepository.flush();

        return guardados
                .stream()
                .map(this::toData)
                .toList();
    }

    @Override
    public Long plazaTrabajador(Long trabajadorId) {
        Trabajador trabajador = trabajadorRepository
                .findById(trabajadorId)
                .orElseThrow(() ->
                        notFound("Trabajador no encontrado")
                );

        if (trabajador.getPlaza() == null) {
            throw bad("El trabajador no tiene plaza");
        }

        return trabajador.getPlaza().getId();
    }

    @Override
    public DistribucionUseCase.ResumenTrabajador resumen(
            Long trabajadorId,
            LocalDate desde,
            LocalDate hasta
    ) {
        Trabajador trabajador = trabajadorRepository
                .findById(trabajadorId)
                .orElseThrow(() ->
                        notFound("Trabajador no encontrado")
                );

        List<DistribucionPersonal> datos =
                distribucionRepository.findByTrabajadorMes(
                        trabajadorId,
                        desde,
                        hasta
                );

        Map<Long, Long> conteos = new LinkedHashMap<>();
        Map<Long, ProgramacionUbicacion> referencias =
                new LinkedHashMap<>();

        for (DistribucionPersonal distribucion : datos) {
            Long ubicacionId =
                    distribucion.getUbicacion().getId();

            referencias.putIfAbsent(
                    ubicacionId,
                    distribucion.getUbicacion()
            );

            conteos.merge(
                    ubicacionId,
                    1L,
                    Long::sum
            );
        }

        List<DistribucionUseCase.ResumenUbicacion> ubicaciones =
                referencias
                        .entrySet()
                        .stream()
                        .map(entry ->
                                new DistribucionUseCase.ResumenUbicacion(
                                        entry.getValue().getCodigo(),
                                        entry.getValue().getNombre(),
                                        conteos.getOrDefault(
                                                entry.getKey(),
                                                0L
                                        )
                                )
                        )
                        .toList();

        return new DistribucionUseCase.ResumenTrabajador(
                trabajador.getId(),
                trabajador.getCodigo(),
                trabajador.getNombreCompleto(),
                ubicaciones
        );
    }

    @Override
    public List<DistribucionUseCase.CoberturaUbicacion> cobertura(
            Long plazaId,
            LocalDate desde,
            LocalDate hasta
    ) {
        List<DistribucionPersonal> datos =
                distribucionRepository.findMes(
                        plazaId,
                        desde,
                        hasta
                );

        Map<Long, Map<LocalDate, Long>> conteos =
                new HashMap<>();

        for (DistribucionPersonal distribucion : datos) {
            conteos
                    .computeIfAbsent(
                            distribucion.getUbicacion().getId(),
                            ignored -> new TreeMap<>()
                    )
                    .merge(
                            distribucion
                                    .getProgramacionTurno()
                                    .getFecha(),
                            1L,
                            Long::sum
                    );
        }

        return ubicacionRepository
                .findByPlazaIdOrderByOrdenAscCodigoAsc(plazaId)
                .stream()
                .filter(ubicacion ->
                        Boolean.TRUE.equals(ubicacion.getActivo())
                                || conteos.containsKey(
                                        ubicacion.getId()
                                )
                )
                .map(ubicacion ->
                        new DistribucionUseCase.CoberturaUbicacion(
                                ubicacion.getId(),
                                ubicacion.getCodigo(),
                                ubicacion.getNombre(),
                                conteos.getOrDefault(
                                        ubicacion.getId(),
                                        Map.of()
                                )
                        )
                )
                .toList();
    }

    private DistribucionUseCase.Distribucion toData(
            DistribucionPersonal distribucion
    ) {
        ProgramacionTurno programacion =
                distribucion.getProgramacionTurno();

        ProgramacionUbicacion ubicacion =
                distribucion.getUbicacion();

        return new DistribucionUseCase.Distribucion(
                distribucion.getId(),
                programacion.getId(),
                programacion.getTrabajador().getId(),
                programacion.getTrabajador().getCodigo(),
                programacion.getTrabajador().getNombreCompleto(),
                programacion.getFecha(),
                programacion.getEstado().name(),
                ubicacion.getId(),
                ubicacion.getCodigo(),
                ubicacion.getNombre(),
                ubicacion.getTipo().name(),
                distribucion.getObservacion()
        );
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                message
        );
    }
}
