package com.sigo.programacion.infrastructure.persistence.adapter;

import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.programacion.application.port.in.DistribucionUseCase;
import com.sigo.programacion.application.port.out.DistribucionGestionPort;
import com.sigo.programacion.infrastructure.persistence.entity.DistribucionPersonal;
import com.sigo.programacion.infrastructure.persistence.entity.EstadoProgramacion;
import com.sigo.programacion.infrastructure.persistence.entity.ProgramacionSecuenciaAgente;
import com.sigo.programacion.infrastructure.persistence.entity.ProgramacionTurno;
import com.sigo.programacion.infrastructure.persistence.entity.ProgramacionUbicacion;
import com.sigo.programacion.infrastructure.persistence.entity.TipoUbicacion;
import com.sigo.programacion.infrastructure.persistence.repository.DistribucionPersonalRepository;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionSecuenciaAgenteRepository;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionTurnoRepository;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionUbicacionRepository;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final ProgramacionSecuenciaAgenteRepository secuenciaRepository;
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
            Long usuarioId,
            boolean forzar
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
                                        || item.programacionTurnoId() == null
                        )) {
            throw bad(
                    "La distribución contiene programaciones inválidas o duplicadas"
            );
        }

        Map<Long, ProgramacionTurno> programacionPorId =
                programacionRepository
                        .findAllParaDistribucion(
                                programacionIds
                        )
                        .stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        ProgramacionTurno::getId,
                                        item -> item
                                )
                        );

        Map<Long, ProgramacionUbicacion> ubicacionPorId =
                ubicacionIds.isEmpty()
                        ? Map.of()
                        : ubicacionRepository
                                .findAllParaDistribucion(
                                        ubicacionIds
                                )
                                .stream()
                                .collect(
                                        java.util.stream.Collectors.toMap(
                                                ProgramacionUbicacion::getId,
                                                item -> item
                                        )
                                );

        if (programacionPorId.size() != programacionIds.size()) {
            throw bad(
                    "Una o más programaciones no existen"
            );
        }

        if (ubicacionPorId.size() != ubicacionIds.size()) {
            throw bad(
                    "Una o más ubicaciones no existen"
            );
        }

        /*
         * Un controlador puede tener ProgramacionTurno, pero la
         * distribución de casetas pertenece exclusivamente a agentes.
         */
        Set<Long> agenteIds =
                trabajadorRepository
                        .findAgentesByPlaza(plazaId)
                        .stream()
                        .map(Trabajador::getId)
                        .collect(
                                java.util.stream.Collectors.toSet()
                        );

        List<ItemResuelto> resueltos =
                new ArrayList<>(
                        distribuciones.size()
                );

        for (DistribucionUseCase.Item item :
                distribuciones) {
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

            if (item.ubicacionId() == null) {
                /*
                 * Una ubicación nula representa explícitamente
                 * "Sin asignar". También permite limpiar una posible
                 * asignación histórica sin crear una nueva.
                 */
                continue;
            }

            if (!agenteIds.contains(
                    programacion.getTrabajador().getId()
            )) {
                throw bad(
                        "Los controladores no pueden recibir asignación de caseta"
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

        Map<Long, DistribucionPersonal> existentePorProgramacion =
                distribucionRepository
                        .findByProgramacionTurnoIdIn(
                                programacionIds
                        )
                        .stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        item ->
                                                item.getProgramacionTurno().getId(),
                                        item -> item
                                )
                        );

        boolean huboDesasignaciones = false;

        for (DistribucionUseCase.Item item :
                distribuciones) {
            if (item.ubicacionId() != null) {
                continue;
            }

            DistribucionPersonal existente =
                    existentePorProgramacion.remove(
                            item.programacionTurnoId()
                    );

            if (existente != null) {
                distribucionRepository.delete(
                        existente
                );
                huboDesasignaciones = true;
            }
        }

        /*
         * Las desasignaciones forman parte del estado final que deben ver
         * las validaciones del lote. Forzamos el flush para que las consultas
         * posteriores no vuelvan a considerar registros eliminados.
         *
         * Esto permite guardar una distribución parcial: los turnos/días
         * todavía no definidos pueden quedar sin caseta y completarse después.
         */
        if (huboDesasignaciones) {
            distribucionRepository.flush();
        }

        /*
         * En modo normal se aplican todas las reglas operativas:
         * habilitación, capacidad, rotación, secuencia y ocupación.
         *
         * En modo forzado se conservan únicamente las validaciones
         * estructurales realizadas arriba (programación existente,
         * plaza correcta, turno operativo, ubicación existente/activa
         * y perteneciente a la plaza), permitiendo registrar una
         * excepción operativa consciente.
         */
        if (!forzar) {
            validarHabilitacionTurnos(
                    plazaId,
                    resueltos
            );

            validarOcupacionFinal(
                    plazaId,
                    resueltos
            );
        }

        List<DistribucionPersonal> guardados =
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

            guardados.add(
                    distribucionRepository.save(
                            distribucion
                    )
            );
        }

        distribucionRepository.flush();

        return guardados
                .stream()
                .map(this::toData)
                .toList();
    }

    private void validarHabilitacionTurnos(
            Long plazaId,
            List<ItemResuelto> resueltos
    ) {
        if (resueltos.isEmpty()) {
            return;
        }

        LocalDate desde = resueltos.stream()
                .map(item -> item.programacion().getFecha())
                .min(LocalDate::compareTo)
                .orElseThrow();

        LocalDate hasta = resueltos.stream()
                .map(item -> item.programacion().getFecha())
                .max(LocalDate::compareTo)
                .orElseThrow();

        List<ProgramacionUbicacion> ubicacionesActivas =
                ubicacionRepository
                        .findByPlazaIdAndActivoTrueOrderByOrdenAscCodigoAsc(
                                plazaId
                        );

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
                programacionRepository.findMes(
                        plazaId,
                        desde,
                        hasta
                )) {
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
                distribucionRepository.findMes(
                        plazaId,
                        desde,
                        hasta
                )) {
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
            Long plazaId,
            List<ItemResuelto> resueltos
    ) {
        if (resueltos.isEmpty()) {
            return;
        }

        LocalDate desde = resueltos.stream()
                .map(item -> item.programacion().getFecha())
                .min(LocalDate::compareTo)
                .orElseThrow();

        LocalDate hasta = resueltos.stream()
                .map(item -> item.programacion().getFecha())
                .max(LocalDate::compareTo)
                .orElseThrow();

        Set<Long> programacionesModificadas =
                resueltos.stream()
                        .map(item -> item.programacion().getId())
                        .collect(java.util.stream.Collectors.toSet());

        Map<String, Set<Long>> ocupadas =
                new HashMap<>();

        for (DistribucionPersonal existente :
                distribucionRepository.findMes(
                        plazaId,
                        desde,
                        hasta
                )) {
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
                distribucionRepository.findMes(
                        plazaId,
                        desde.minusDays(1),
                        hasta
                )) {
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
                                + " tiene dos días seguidos en AUXILIAR/APOYO: "
                                + actual.getFecha().minusDays(1)
                                + " = "
                                + ubicacionAnterior.getCodigo()
                                + " y "
                                + actual.getFecha()
                                + " = "
                                + resuelto.ubicacion().getCodigo()
                                + ". Cambia una de esas dos asignaciones por una VIA."
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

        List<ProgramacionUbicacion> ubicacionesActivas =
                ubicacionRepository
                        .findByPlazaIdAndActivoTrueOrderByOrdenAscCodigoAsc(
                                plazaId
                        );

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
    public List<DistribucionUseCase.MatrizItem> listarMatriz(
            Long plazaId,
            LocalDate desde,
            LocalDate hasta
    ) {
        List<ProgramacionTurnoRepository.TurnoResumen> programaciones =
                programacionRepository.findMesResumen(
                        plazaId,
                        desde,
                        hasta
                );

        Map<Long, DistribucionPersonal> distribucionPorProgramacion =
                distribucionRepository.findMes(
                                plazaId,
                                desde,
                                hasta
                        )
                        .stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        item ->
                                                item.getProgramacionTurno().getId(),
                                        item ->
                                                item,
                                        (a, b) ->
                                                a
                                )
                        );

        List<ProgramacionSecuenciaAgente> secuencias =
                secuenciaRepository
                        .findActivasByPlazaId(
                                plazaId
                        )
                        .stream()
                        .filter(
                                secuencia ->
                                        secuencia.getGrupo() != null
                        )
                        .sorted(
                                Comparator
                                        .<ProgramacionSecuenciaAgente>comparingInt(
                                                (ProgramacionSecuenciaAgente secuencia) ->
                                                        ordenGrupo(
                                                                secuencia.getGrupo()
                                                        )
                                        )
                                        .thenComparingInt(
                                                (ProgramacionSecuenciaAgente secuencia) ->
                                                        secuencia.getOrden() == null
                                                                ? Integer.MAX_VALUE
                                                                : secuencia.getOrden()
                                        )
                        )
                        .toList();

        Map<Long, Integer> ordenTrabajador =
                new HashMap<>();

        for (
                int indice = 0;
                indice < secuencias.size();
                indice++
        ) {
            ordenTrabajador.put(
                    secuencias
                            .get(indice)
                            .getAgente()
                            .getId(),
                    indice
            );
        }

        return programaciones
                .stream()
                /*
                 * La pantalla de distribución solo muestra agentes que
                 * pertenecen a una secuencia. El PDF debe conservar ese
                 * mismo conjunto y exactamente el mismo orden visual.
                 */
                .filter(
                        programacion ->
                                ordenTrabajador.containsKey(
                                        programacion.getTrabajadorId()
                                )
                )
                .sorted(
                        Comparator
                                .<ProgramacionTurnoRepository.TurnoResumen>comparingInt(
                                        (ProgramacionTurnoRepository.TurnoResumen programacion) ->
                                                ordenTrabajador.getOrDefault(
                                                        programacion.getTrabajadorId(),
                                                        Integer.MAX_VALUE
                                                )
                                )
                                .thenComparing(
                                        ProgramacionTurnoRepository
                                                .TurnoResumen::getFecha
                                )
                )
                .map(programacion -> {
                    DistribucionPersonal distribucion =
                            distribucionPorProgramacion.get(
                                    programacion.getProgramacionId()
                            );

                    String ubicacionCodigo =
                            distribucion == null
                                    ? null
                                    : distribucion
                                            .getUbicacion()
                                            .getCodigo();

                    String ubicacionTipo =
                            distribucion == null
                                    ? null
                                    : distribucion
                                            .getUbicacion()
                                            .getTipo()
                                            .name();

                    return new DistribucionUseCase.MatrizItem(
                            programacion.getProgramacionId(),
                            programacion.getTrabajadorId(),
                            programacion.getCodigoTrabajador(),
                            programacion.getNombreTrabajador(),
                            programacion.getPlazaCodigo(),
                            programacion.getFecha(),
                            programacion.getEstado().name(),
                            ubicacionCodigo,
                            ubicacionTipo
                    );
                })
                .toList();
    }


    private int ordenGrupo(
            com.sigo.programacion.infrastructure.persistence.entity.GrupoProgramacion grupo
    ) {
        return switch (
                grupo
        ) {
            case SECUENCIA_1 ->
                    1;

            case SECUENCIA_2 ->
                    2;

            case SECUENCIA_3 ->
                    3;

            case SECUENCIA_4 ->
                    4;

            case PART_TIME ->
                    5;
        };
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
