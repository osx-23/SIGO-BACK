package com.sigo.programacion.infrastructure.persistence.adapter;

import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.programacion.application.port.in.DistribucionUseCase;
import com.sigo.programacion.application.port.out.DistribucionGestionPort;
import com.sigo.programacion.infrastructure.persistence.entity.DistribucionPersonal;
import com.sigo.programacion.infrastructure.persistence.entity.EstadoProgramacion;
import com.sigo.programacion.infrastructure.persistence.entity.ProgramacionTurno;
import com.sigo.programacion.infrastructure.persistence.entity.ProgramacionUbicacion;
import com.sigo.programacion.infrastructure.persistence.entity.TipoUbicacion;
import com.sigo.programacion.infrastructure.persistence.repository.DistribucionPersonalRepository;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionTurnoRepository;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionUbicacionRepository;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

        Set<Long> programacionIds =
                distribuciones.stream()
                        .filter(Objects::nonNull)
                        .map(DistribucionUseCase.Item::programacionTurnoId)
                        .filter(Objects::nonNull)
                        .collect(
                                java.util.stream.Collectors.toSet()
                        );

        Set<Long> ubicacionIds =
                distribuciones.stream()
                        .filter(Objects::nonNull)
                        .map(DistribucionUseCase.Item::ubicacionId)
                        .filter(Objects::nonNull)
                        .collect(
                                java.util.stream.Collectors.toSet()
                        );

        if (programacionIds.size() != distribuciones.size()
                || distribuciones.stream()
                        .anyMatch(item ->
                                item == null
                                        || item.ubicacionId() == null
                        )) {
            throw bad(
                    "La distribución contiene datos incompletos o programaciones duplicadas"
            );
        }

        Map<Long, ProgramacionTurno> programacionPorId =
                programacionRepository
                        .findAllById(programacionIds)
                        .stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        ProgramacionTurno::getId,
                                        item -> item
                                )
                        );

        if (programacionPorId.size()
                != programacionIds.size()) {
            throw bad(
                    "Una o más programaciones no existen"
            );
        }

        Map<Long, ProgramacionUbicacion> ubicacionPorId =
                ubicacionRepository
                        .findAllById(ubicacionIds)
                        .stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        ProgramacionUbicacion::getId,
                                        item -> item
                                )
                        );

        if (ubicacionPorId.size()
                != ubicacionIds.size()) {
            throw bad(
                    "Una o más ubicaciones no existen"
            );
        }

        List<ItemResuelto> resueltos =
                new ArrayList<>(
                        distribuciones.size()
                );

        for (DistribucionUseCase.Item item :
                distribuciones) {
            if (item == null
                    || item.programacionTurnoId() == null
                    || item.ubicacionId() == null) {
                throw bad(
                        "La distribución contiene datos incompletos"
                );
            }

            ProgramacionTurno programacion =
                    programacionPorId.get(
                            item.programacionTurnoId()
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
                    ubicacionPorId.get(
                            item.ubicacionId()
                    );

            if (!Boolean.TRUE.equals(
                    ubicacion.getActivo()
            )) {
                throw bad(
                        "Ubicación no válida"
                );
            }

            if (!Objects.equals(
                    ubicacion.getPlaza().getId(),
                    plazaId
            )) {
                throw bad(
                        "La ubicación no pertenece a la plaza"
                );
            }

            resueltos.add(
                    new ItemResuelto(
                            item,
                            programacion,
                            ubicacion
                    )
            );
        }

        LocalDate desde =
                resueltos.stream()
                        .map(item ->
                                item.programacion().getFecha()
                        )
                        .min(LocalDate::compareTo)
                        .orElseThrow();

        LocalDate hasta =
                resueltos.stream()
                        .map(item ->
                                item.programacion().getFecha()
                        )
                        .max(LocalDate::compareTo)
                        .orElseThrow();

        List<ProgramacionUbicacion> ubicacionesActivas =
                ubicacionRepository
                        .findByPlazaIdAndActivoTrueOrderByOrdenAscCodigoAsc(
                                plazaId
                        );

        List<ProgramacionTurno> programacionesPeriodo =
                programacionRepository.findMes(
                        plazaId,
                        desde,
                        hasta
                );

        List<DistribucionPersonal> distribucionesPeriodoConAnterior =
                distribucionRepository.findMes(
                        plazaId,
                        desde.minusDays(1),
                        hasta
                );

        validarHabilitacionTurnos(
                resueltos,
                desde,
                programacionesPeriodo,
                distribucionesPeriodoConAnterior,
                ubicacionesActivas
        );

        validarOcupacionFinal(
                resueltos,
                desde,
                distribucionesPeriodoConAnterior,
                ubicacionesActivas
        );

        Map<Long, DistribucionPersonal> existentePorProgramacion =
                distribucionesPeriodoConAnterior
                        .stream()
                        .filter(item ->
                                programacionIds.contains(
                                        item.getProgramacionTurno().getId()
                                )
                        )
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        item ->
                                                item.getProgramacionTurno().getId(),
                                        item -> item
                                )
                        );

        List<DistribucionPersonal> paraGuardar =
                new ArrayList<>(
                        resueltos.size()
                );

        for (ItemResuelto resuelto :
                resueltos) {
            DistribucionUseCase.Item item =
                    resuelto.item();

            ProgramacionTurno programacion =
                    resuelto.programacion();

            DistribucionPersonal distribucion =
                    existentePorProgramacion.get(
                            programacion.getId()
                    );

            if (distribucion == null) {
                distribucion =
                        new DistribucionPersonal();

                distribucion.setProgramacionTurno(
                        programacion
                );

                distribucion.setAsignadoPor(
                        actual
                );
            }

            distribucion.setUbicacion(
                    resuelto.ubicacion()
            );

            distribucion.setObservacion(
                    item.observacion()
            );

            distribucion.setActualizadoPor(
                    actual
            );

            paraGuardar.add(
                    distribucion
            );
        }

        List<DistribucionPersonal> guardados =
                distribucionRepository
                        .saveAllAndFlush(
                                paraGuardar
                        );

        return guardados
                .stream()
                .map(this::toData)
                .toList();
    }

    private void validarHabilitacionTurnos(
            List<ItemResuelto> resueltos,
            LocalDate desde,
            List<ProgramacionTurno> programacionesPeriodo,
            List<DistribucionPersonal> distribucionesPeriodoConAnterior,
            List<ProgramacionUbicacion> ubicacionesActivas
    ) {
        if (resueltos.isEmpty()) {
            return;
        }

        Map<EstadoProgramacion, Integer> capacidadPorTurno =
                new java.util.EnumMap<>(
                        EstadoProgramacion.class
                );

        for (EstadoProgramacion estado :
                List.of(
                        EstadoProgramacion.A,
                        EstadoProgramacion.B,
                        EstadoProgramacion.C
                )) {
            int capacidad =
                    (int) ubicacionesActivas.stream()
                            .filter(item ->
                                    permiteTurno(
                                            item,
                                            estado
                                    )
                            )
                            .count();

            capacidadPorTurno.put(
                    estado,
                    capacidad
            );
        }

        Map<String, Integer> demandaPorTurno =
                new HashMap<>();

        Map<String, EstadoProgramacion> estadoPorClave =
                new HashMap<>();

        for (ProgramacionTurno programacion :
                programacionesPeriodo) {
            if (!programacion.getEstado().esOperativo()) {
                continue;
            }

            String clave =
                    claveTurno(programacion);

            demandaPorTurno.merge(
                    clave,
                    1,
                    Integer::sum
            );

            estadoPorClave.put(
                    clave,
                    programacion.getEstado()
            );
        }

        Set<Long> programacionesModificadas =
                resueltos.stream()
                        .map(item ->
                                item.programacion().getId()
                        )
                        .collect(
                                java.util.stream.Collectors.toSet()
                        );

        Map<String, Integer> overflowUsado =
                new HashMap<>();

        Map<String, Set<Long>> ubicacionesFinalesPorTurno =
                new HashMap<>();

        for (DistribucionPersonal existente :
                distribucionesPeriodoConAnterior) {
            if (existente.getProgramacionTurno()
                    .getFecha()
                    .isBefore(desde)) {
                continue;
            }

            if (programacionesModificadas.contains(
                    existente.getProgramacionTurno().getId()
            )) {
                continue;
            }

            ProgramacionTurno programacion =
                    existente.getProgramacionTurno();

            String clave =
                    claveTurno(programacion);

            ubicacionesFinalesPorTurno
                    .computeIfAbsent(
                            clave,
                            ignored ->
                                    new java.util.LinkedHashSet<>()
                    )
                    .add(
                            existente.getUbicacion().getId()
                    );

            if (!permiteTurno(
                    existente.getUbicacion(),
                    programacion.getEstado()
            )) {
                if (programacion.getEstado()
                        == EstadoProgramacion.C
                        && existente.getUbicacion().getTipo()
                        != TipoUbicacion.VIA) {
                    throw bad(
                            "En turno C las casetas adicionales deben ser vías"
                    );
                }

                overflowUsado.merge(
                        clave,
                        1,
                        Integer::sum
                );
            }
        }

        for (ItemResuelto resuelto :
                resueltos) {
            String clave =
                    claveTurno(
                            resuelto.programacion()
                    );

            ubicacionesFinalesPorTurno
                    .computeIfAbsent(
                            clave,
                            ignored ->
                                    new java.util.LinkedHashSet<>()
                    )
                    .add(
                            resuelto.ubicacion().getId()
                    );

            if (!permiteTurno(
                    resuelto.ubicacion(),
                    resuelto.programacion().getEstado()
            )) {
                if (resuelto.programacion().getEstado()
                        == EstadoProgramacion.C
                        && resuelto.ubicacion().getTipo()
                        != TipoUbicacion.VIA) {
                    throw bad(
                            "En turno C las casetas adicionales deben ser vías"
                    );
                }

                overflowUsado.merge(
                        clave,
                        1,
                        Integer::sum
                );
            }
        }

        for (Map.Entry<String, Integer> entry :
                overflowUsado.entrySet()) {
            EstadoProgramacion estado =
                    estadoPorClave.get(
                            entry.getKey()
                    );

            if (estado == null) {
                continue;
            }

            int demanda =
                    demandaPorTurno.getOrDefault(
                            entry.getKey(),
                            0
                    );

            int capacidad =
                    capacidadPorTurno.getOrDefault(
                            estado,
                            0
                    );

            int maxOverflow =
                    Math.max(
                            0,
                            demanda - capacidad
                    );

            if (entry.getValue() > 0) {
                Set<Long> asignadas =
                        ubicacionesFinalesPorTurno
                                .getOrDefault(
                                        entry.getKey(),
                                        Set.of()
                                );

                boolean habilitadasCompletas;

                if (estado
                        == EstadoProgramacion.C) {
                    habilitadasCompletas =
                            ubicacionesActivas.stream()
                                    .filter(item ->
                                            item.getTipo()
                                                    == TipoUbicacion.VIA
                                    )
                                    .filter(item ->
                                            permiteTurno(
                                                    item,
                                                    EstadoProgramacion.C
                                            )
                                    )
                                    .allMatch(item ->
                                            asignadas.contains(
                                                    item.getId()
                                            )
                                    );
                } else {
                    habilitadasCompletas =
                            ubicacionesActivas.stream()
                                    .filter(item ->
                                            permiteTurno(
                                                    item,
                                                    estado
                                            )
                                    )
                                    .allMatch(item ->
                                            asignadas.contains(
                                                    item.getId()
                                            )
                                    );
                }

                if (!habilitadasCompletas) {
                    throw bad(
                            "Primero deben ocuparse todas las casetas habilitadas del turno "
                                    + estado.name()
                                    + " antes de usar una caseta adicional"
                    );
                }
            }

            if (entry.getValue() > maxOverflow) {
                throw bad(
                        "Solo se pueden habilitar casetas adicionales cuando la cantidad de agentes del turno "
                                + estado.name()
                                + " supera las "
                                + capacidad
                                + " casetas habilitadas. Para esta fecha se permiten como máximo "
                                + maxOverflow
                                + " caseta(s) adicional(es)"
                );
            }
        }
    }

    private void validarOcupacionFinal(
            List<ItemResuelto> resueltos,
            LocalDate desde,
            List<DistribucionPersonal> distribucionesPeriodoConAnterior,
            List<ProgramacionUbicacion> ubicacionesActivas
    ) {
        if (resueltos.isEmpty()) {
            return;
        }

        Set<Long> programacionesModificadas =
                resueltos.stream()
                        .map(item -> item.programacion().getId())
                        .collect(java.util.stream.Collectors.toSet());

        Map<String, Set<Long>> ocupadas =
                new HashMap<>();

        for (DistribucionPersonal existente :
                distribucionesPeriodoConAnterior) {
            if (existente.getProgramacionTurno()
                    .getFecha()
                    .isBefore(desde)) {
                continue;
            }

            if (programacionesModificadas.contains(
                    existente.getProgramacionTurno().getId()
            )) {
                continue;
            }

            String key = claveTurno(
                    existente.getProgramacionTurno()
            );

            Set<Long> ids =
                    ocupadas.computeIfAbsent(
                            key,
                            ignored -> new java.util.LinkedHashSet<>()
                    );

            if (!ids.add(existente.getUbicacion().getId())) {
                throw bad(
                        "Existe una ubicación duplicada en la distribución guardada"
                );
            }
        }

        for (ItemResuelto resuelto : resueltos) {
            String key =
                    claveTurno(resuelto.programacion());

            Set<Long> ids =
                    ocupadas.computeIfAbsent(
                            key,
                            ignored -> new java.util.LinkedHashSet<>()
                    );

            if (!ids.add(resuelto.ubicacion().getId())) {
                throw bad(
                        "La ubicación "
                                + resuelto.ubicacion().getCodigo()
                                + " ya está asignada para el turno "
                                + resuelto.programacion().getEstado().name()
                                + " del "
                                + resuelto.programacion().getFecha()
                );
            }
        }

        Map<String, Long> asignacionPorAgenteDiaTurno =
                new HashMap<>();

        Map<String, ProgramacionUbicacion> ubicacionPorAgenteDia =
                new HashMap<>();

        for (DistribucionPersonal existente :
                distribucionesPeriodoConAnterior) {
            if (programacionesModificadas.contains(
                    existente.getProgramacionTurno().getId()
            )) {
                continue;
            }

            ProgramacionTurno turnoExistente =
                    existente.getProgramacionTurno();

            if (!turnoExistente.getEstado().esOperativo()) {
                continue;
            }

            asignacionPorAgenteDiaTurno.put(
                    claveAgenteDiaTurno(
                            turnoExistente
                    ),
                    existente.getUbicacion().getId()
            );

            ubicacionPorAgenteDia.put(
                    claveAgenteDia(
                            turnoExistente
                    ),
                    existente.getUbicacion()
            );
        }

        for (ItemResuelto resuelto : resueltos) {
            asignacionPorAgenteDiaTurno.put(
                    claveAgenteDiaTurno(
                            resuelto.programacion()
                    ),
                    resuelto.ubicacion().getId()
            );

            ubicacionPorAgenteDia.put(
                    claveAgenteDia(
                            resuelto.programacion()
                    ),
                    resuelto.ubicacion()
            );
        }

        for (ItemResuelto resuelto : resueltos) {
            ProgramacionTurno actual =
                    resuelto.programacion();

            ProgramacionUbicacion ubicacionAnterior =
                    ubicacionPorAgenteDia.get(
                            actual.getTrabajador().getId()
                                    + "|"
                                    + actual.getFecha().minusDays(1)
                    );

            if (actual.getEstado()
                    == EstadoProgramacion.C
                    && resuelto.ubicacion().getTipo()
                    == TipoUbicacion.VIA
                    && ubicacionAnterior != null
                    && ubicacionAnterior.getTipo()
                    == TipoUbicacion.VIA
                    && Objects.equals(
                            ubicacionAnterior.getId(),
                            resuelto.ubicacion().getId()
                    )) {
                throw bad(
                        "En turno C el agente "
                                + actual.getTrabajador().getNombreCompleto()
                                + " no puede repetir la misma vía que tuvo el día anterior"
                );
            }

            if (esApoyoOAuxiliar(
                    resuelto.ubicacion()
            ) && esApoyoOAuxiliar(
                    ubicacionAnterior
            )) {
                throw bad(
                        "El agente "
                                + actual.getTrabajador().getNombreCompleto()
                                + " no puede estar dos días seguidos en una ubicación de tipo APOYO o AUXILIAR"
                );
            }
        }

        for (ItemResuelto resuelto : resueltos) {
            ProgramacionTurno actual =
                    resuelto.programacion();

            String claveAnterior =
                    actual.getTrabajador().getId()
                            + "|"
                            + actual.getFecha().minusDays(1)
                            + "|"
                            + actual.getEstado().name();

            Long ubicacionAnterior =
                    asignacionPorAgenteDiaTurno.get(
                            claveAnterior
                    );

            if (Objects.equals(
                    ubicacionAnterior,
                    resuelto.ubicacion().getId()
            )) {
                throw bad(
                        "El agente "
                                + actual.getTrabajador().getNombreCompleto()
                                + " no puede repetir la ubicación "
                                + resuelto.ubicacion().getCodigo()
                                + " en días consecutivos manteniendo el turno "
                                + actual.getEstado().name()
                );
            }
        }

        Map<String, EstadoProgramacion> turnoPorClave =
                new HashMap<>();

        for (ItemResuelto resuelto : resueltos) {
            turnoPorClave.put(
                    claveTurno(resuelto.programacion()),
                    resuelto.programacion().getEstado()
            );
        }

        for (Map.Entry<String, EstadoProgramacion> entry :
                turnoPorClave.entrySet()) {
            Set<Long> ids =
                    ocupadas.getOrDefault(
                            entry.getKey(),
                            Set.of()
                    );

            boolean tieneAuxiliarAsignada =
                    ubicacionesActivas.stream()
                            .filter(item ->
                                    item.getTipo() == TipoUbicacion.AUXILIAR
                            )
                            .filter(item ->
                                    permiteTurno(
                                            item,
                                            entry.getValue()
                                    )
                            )
                            .anyMatch(item ->
                                    ids.contains(item.getId())
                            );

            if (tieneAuxiliarAsignada) {
                boolean viasCompletas =
                        ubicacionesActivas.stream()
                                .filter(item ->
                                        item.getTipo() == TipoUbicacion.VIA
                                )
                                .filter(item ->
                                        permiteTurno(
                                                item,
                                                entry.getValue()
                                        )
                                )
                                .allMatch(item ->
                                        ids.contains(item.getId())
                                );

                if (!viasCompletas) {
                    throw bad(
                            "No se pueden asignar ubicaciones auxiliares mientras existan vías habilitadas sin cubrir en el turno "
                                    + entry.getValue().name()
                    );
                }
            }

            List<ProgramacionUbicacion> secuenciales =
                    ubicacionesActivas.stream()
                            .filter(item ->
                                    item.getTipo() != TipoUbicacion.VIA
                            )
                            .filter(item ->
                                    permiteTurno(
                                            item,
                                            entry.getValue()
                                    )
                            )
                            .sorted(
                                    java.util.Comparator
                                            .comparing(
                                                    ProgramacionUbicacion::getOrden,
                                                    java.util.Comparator.nullsLast(
                                                            Integer::compareTo
                                                    )
                                            )
                                            .thenComparing(
                                                    ProgramacionUbicacion::getCodigo,
                                                    String.CASE_INSENSITIVE_ORDER
                                            )
                            )
                            .toList();

            boolean faltaAnterior = false;

            for (ProgramacionUbicacion ubicacion :
                    secuenciales) {
                boolean asignada =
                        ids.contains(ubicacion.getId());

                if (!asignada) {
                    faltaAnterior = true;
                    continue;
                }

                if (faltaAnterior) {
                    throw bad(
                            "No se puede asignar "
                                    + ubicacion.getCodigo()
                                    + " sin completar antes las ubicaciones auxiliares/apoyo anteriores del turno "
                                    + entry.getValue().name()
                    );
                }
            }
        }
    }

    private boolean permiteTurno(
            ProgramacionUbicacion ubicacion,
            EstadoProgramacion turno
    ) {
        return switch (turno) {
            case A -> Boolean.TRUE.equals(
                    ubicacion.getPermiteTurnoA()
            );
            case B -> Boolean.TRUE.equals(
                    ubicacion.getPermiteTurnoB()
            );
            case C -> Boolean.TRUE.equals(
                    ubicacion.getPermiteTurnoC()
            );
            default -> false;
        };
    }

    private String claveTurno(
            ProgramacionTurno programacion
    ) {
        return programacion.getFecha()
                + "|"
                + programacion.getEstado().name();
    }

    private String claveAgenteDia(
            ProgramacionTurno programacion
    ) {
        return programacion.getTrabajador().getId()
                + "|"
                + programacion.getFecha();
    }

    private boolean esApoyoOAuxiliar(
            ProgramacionUbicacion ubicacion
    ) {
        return ubicacion != null
                && (
                        ubicacion.getTipo() == TipoUbicacion.AUXILIAR
                                || ubicacion.getTipo() == TipoUbicacion.APOYO
                );
    }

    private String claveAgenteDiaTurno(
            ProgramacionTurno programacion
    ) {
        return programacion.getTrabajador().getId()
                + "|"
                + programacion.getFecha()
                + "|"
                + programacion.getEstado().name();
    }

    private record ItemResuelto(
            DistribucionUseCase.Item item,
            ProgramacionTurno programacion,
            ProgramacionUbicacion ubicacion
    ) {
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
    public List<DistribucionUseCase.Distribucion> listarTrabajador(
            Long trabajadorId,
            LocalDate desde,
            LocalDate hasta
    ) {
        return distribucionRepository
                .findByTrabajadorMes(
                        trabajadorId,
                        desde,
                        hasta
                )
                .stream()
                .map(this::toData)
                .toList();
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

    private BusinessException bad(String message) {
        return new BusinessException(message);
    }

    private ResourceNotFoundException notFound(String message) {
        return new ResourceNotFoundException(message);
    }
}
