package com.sigo.programacion.infrastructure.service;

import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.programacion.application.port.in.GeneradorAsignacionCasetasUseCase;
import com.sigo.programacion.application.port.in.GeneradorAsignacionCasetasUseCase.CasetaConfig;
import com.sigo.programacion.application.port.in.GeneradorAsignacionCasetasUseCase.Configuracion;
import com.sigo.programacion.application.port.in.GeneradorAsignacionCasetasUseCase.Conflicto;
import com.sigo.programacion.application.port.in.GeneradorAsignacionCasetasUseCase.ItemPropuesta;
import com.sigo.programacion.application.port.in.GeneradorAsignacionCasetasUseCase.Propuesta;
import com.sigo.programacion.application.port.in.GeneradorAsignacionCasetasUseCase.Restriccion;
import com.sigo.programacion.application.port.in.GeneradorAsignacionCasetasUseCase.TipoPeriodo;
import com.sigo.programacion.domain.GrupoFlujoCaseta;
import com.sigo.programacion.domain.ProgramacionValidationException;
import com.sigo.programacion.infrastructure.persistence.entity.*;
import com.sigo.programacion.infrastructure.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GeneradorAsignacionCasetasJpaService implements GeneradorAsignacionCasetasUseCase {

    private final ConfiguracionAsignacionCasetaRepository configuracionRepository;
    private final ConfiguracionCasetaRepository casetaConfiguracionRepository;
    private final AgenteCasetaRestriccionRepository restriccionRepository;
    private final ProgramacionUbicacionRepository ubicacionRepository;
    private final ProgramacionTurnoRepository turnoRepository;
    private final DistribucionPersonalRepository distribucionRepository;
    private final PlazaRepository plazaRepository;
    private final TrabajadorRepository trabajadorRepository;

    @Transactional(readOnly = true)
    public Configuracion obtenerConfiguracion(Long plazaId) {
        validarPlaza(plazaId);
        return toConfiguracion(
                configuracionRepository.findByPlazaId(plazaId)
                        .orElseGet(() -> configuracionPorDefecto(plazaId))
        );
    }

    @Transactional
    public Configuracion guardarConfiguracion(
            Long plazaId,
            int maxSemana,
            int maxMes,
            int maxConsecutivos,
            boolean balancearFlujo
    ) {
        if (maxSemana < 1 || maxMes < 1 || maxConsecutivos < 1) {
            throw new ProgramacionValidationException(
                    "Los máximos deben ser mayores a cero"
            );
        }
        if (maxMes < maxSemana) {
            throw new ProgramacionValidationException(
                    "El máximo mensual no puede ser menor al máximo semanal"
            );
        }

        Plaza plaza = validarPlaza(plazaId);
        ConfiguracionAsignacionCaseta entity =
                configuracionRepository.findByPlazaId(plazaId)
                        .orElseGet(ConfiguracionAsignacionCaseta::new);

        entity.setPlaza(plaza);
        entity.setMaxMismaCasetaSemana(maxSemana);
        entity.setMaxMismaCasetaMes(maxMes);
        entity.setMaxConsecutivos(maxConsecutivos);
        entity.setBalancearFlujo(balancearFlujo);

        return toConfiguracion(configuracionRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<CasetaConfig> listarCasetas(Long plazaId) {
        validarPlaza(plazaId);
        Map<Long, ConfiguracionCaseta> configuradas =
                casetaConfiguracionRepository.findByUbicacionPlazaId(plazaId)
                        .stream()
                        .collect(Collectors.toMap(
                                item -> item.getUbicacion().getId(),
                                Function.identity()
                        ));

        return ubicacionRepository
                .findByPlazaIdOrderByOrdenAscCodigoAsc(plazaId)
                .stream()
                .map(ubicacion -> {
                    ConfiguracionCaseta config = configuradas.get(ubicacion.getId());
                    return new CasetaConfig(
                            ubicacion.getId(),
                            ubicacion.getCodigo(),
                            ubicacion.getNombre(),
                            config == null
                                    ? GrupoFlujoCaseta.SIN_CLASIFICAR
                                    : config.getGrupoFlujo(),
                            config == null ? null : config.getMaxSemana(),
                            config == null ? null : config.getMaxMes(),
                            Boolean.TRUE.equals(ubicacion.getActivo())
                    );
                })
                .toList();
    }

    @Transactional
    public CasetaConfig guardarCaseta(
            Long plazaId,
            Long ubicacionId,
            GrupoFlujoCaseta grupo,
            Integer maxSemana,
            Integer maxMes
    ) {
        validarPlaza(plazaId);

        ProgramacionUbicacion ubicacion =
                ubicacionRepository.findById(ubicacionId)
                        .orElseThrow(() ->
                                new ProgramacionValidationException(
                                        "La caseta indicada no existe"
                                )
                        );

        if (!Objects.equals(ubicacion.getPlaza().getId(), plazaId)) {
            throw new ProgramacionValidationException(
                    "La caseta no pertenece a la plaza seleccionada"
            );
        }

        if (maxSemana != null && maxSemana < 1
                || maxMes != null && maxMes < 1) {
            throw new ProgramacionValidationException(
                    "Los máximos de caseta deben ser mayores a cero"
            );
        }

        ConfiguracionCaseta entity =
                casetaConfiguracionRepository.findByUbicacionId(ubicacionId)
                        .orElseGet(ConfiguracionCaseta::new);

        entity.setUbicacion(ubicacion);
        entity.setGrupoFlujo(
                grupo == null
                        ? GrupoFlujoCaseta.SIN_CLASIFICAR
                        : grupo
        );
        entity.setMaxSemana(maxSemana);
        entity.setMaxMes(maxMes);

        entity = casetaConfiguracionRepository.save(entity);

        return new CasetaConfig(
                ubicacion.getId(),
                ubicacion.getCodigo(),
                ubicacion.getNombre(),
                entity.getGrupoFlujo(),
                entity.getMaxSemana(),
                entity.getMaxMes(),
                Boolean.TRUE.equals(ubicacion.getActivo())
        );
    }

    @Transactional(readOnly = true)
    public List<Restriccion> listarRestricciones(Long plazaId) {
        validarPlaza(plazaId);
        return restriccionRepository
                .findByTrabajadorPlazaIdAndActivoTrue(plazaId)
                .stream()
                .map(this::toRestriccion)
                .sorted(
                        Comparator.comparing(
                                Restriccion::trabajador,
                                String.CASE_INSENSITIVE_ORDER
                        ).thenComparing(Restriccion::ubicacionCodigo)
                )
                .toList();
    }

    @Transactional
    public Restriccion guardarRestriccion(
            Long plazaId,
            Long trabajadorId,
            Long ubicacionId,
            String motivo,
            boolean activo
    ) {
        validarPlaza(plazaId);

        Trabajador trabajador =
                trabajadorRepository.findById(trabajadorId)
                        .orElseThrow(() ->
                                new ProgramacionValidationException(
                                        "El agente indicado no existe"
                                )
                        );

        ProgramacionUbicacion ubicacion =
                ubicacionRepository.findById(ubicacionId)
                        .orElseThrow(() ->
                                new ProgramacionValidationException(
                                        "La caseta indicada no existe"
                                )
                        );

        if (trabajador.getPlaza() == null
                || !Objects.equals(trabajador.getPlaza().getId(), plazaId)
                || !Objects.equals(ubicacion.getPlaza().getId(), plazaId)) {
            throw new ProgramacionValidationException(
                    "El agente y la caseta deben pertenecer a la misma plaza"
            );
        }

        AgenteCasetaRestriccion entity =
                restriccionRepository
                        .findByTrabajadorIdAndUbicacionId(
                                trabajadorId,
                                ubicacionId
                        )
                        .orElseGet(AgenteCasetaRestriccion::new);

        entity.setTrabajador(trabajador);
        entity.setUbicacion(ubicacion);
        entity.setMotivo(
                motivo == null || motivo.isBlank()
                        ? null
                        : motivo.trim()
        );
        entity.setActivo(activo);

        return toRestriccion(restriccionRepository.save(entity));
    }

    @Transactional
    public void eliminarRestriccion(Long restriccionId) {
        AgenteCasetaRestriccion entity =
                restriccionRepository.findById(restriccionId)
                        .orElseThrow(() ->
                                new ProgramacionValidationException(
                                        "La restricción no existe"
                                )
                        );
        entity.setActivo(false);
        restriccionRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public Propuesta generar(
            Long plazaId,
            int anio,
            int mes,
            TipoPeriodo periodo,
            Integer semana
    ) {
        validarPlaza(plazaId);

        YearMonth ym;
        try {
            ym = YearMonth.of(anio, mes);
        } catch (Exception exception) {
            throw new ProgramacionValidationException("Año o mes inválido");
        }

        LocalDate desde = ym.atDay(1);
        LocalDate hasta = ym.atEndOfMonth();

        if (periodo == TipoPeriodo.SEMANA) {
            if (semana == null || semana < 1 || semana > 5) {
                throw new ProgramacionValidationException(
                        "Debe seleccionar una semana entre 1 y 5"
                );
            }
            int inicio = ((semana - 1) * 7) + 1;
            if (inicio > ym.lengthOfMonth()) {
                throw new ProgramacionValidationException(
                        "La semana seleccionada no existe en este mes"
                );
            }
            desde = ym.atDay(inicio);
            hasta = ym.atDay(Math.min(inicio + 6, ym.lengthOfMonth()));
        }

        Configuracion config = obtenerConfiguracion(plazaId);

        List<ProgramacionUbicacion> ubicaciones =
                ubicacionRepository
                        .findByPlazaIdAndActivoTrueOrderByOrdenAscCodigoAsc(plazaId);

        if (ubicaciones.isEmpty()) {
            throw new ProgramacionValidationException(
                    "La plaza no tiene casetas activas configuradas"
            );
        }

        Map<Long, ConfiguracionCaseta> configCaseta =
                casetaConfiguracionRepository.findByUbicacionPlazaId(plazaId)
                        .stream()
                        .collect(Collectors.toMap(
                                item -> item.getUbicacion().getId(),
                                Function.identity()
                        ));

        Set<String> restricciones =
                restriccionRepository
                        .findByTrabajadorPlazaIdAndActivoTrue(plazaId)
                        .stream()
                        .map(item ->
                                key(
                                        item.getTrabajador().getId(),
                                        item.getUbicacion().getId()
                                )
                        )
                        .collect(Collectors.toSet());

        List<ProgramacionTurno> turnos =
                new ArrayList<>(
                        turnoRepository.findMes(plazaId, desde, hasta)
                                .stream()
                                .filter(item ->
                                        item.getEstado() == EstadoProgramacion.A
                                                || item.getEstado() == EstadoProgramacion.B
                                                || item.getEstado() == EstadoProgramacion.C
                                )
                                .toList()
                );

        /*
         * Primero mezclamos para que, dentro del mismo día y turno,
         * los agentes no se procesen siempre en el mismo orden.
         * Luego ordenamos solo por fecha y turno. El sort es estable,
         * así que mantiene el orden aleatorio entre agentes equivalentes.
         */
        Collections.shuffle(turnos);
        turnos.sort(
                Comparator.comparing(ProgramacionTurno::getFecha)
                        .thenComparing(item -> item.getEstado().name())
        );

        if (turnos.isEmpty()) {
            throw new ProgramacionValidationException(
                    "No hay agentes programados en turnos A, B o C para el periodo"
            );
        }

        Set<Long> turnosObjetivo =
                turnos.stream()
                        .map(ProgramacionTurno::getId)
                        .collect(Collectors.toSet());

        List<DistribucionPersonal> existentesMes =
                distribucionRepository.findMes(
                        plazaId,
                        ym.atDay(1),
                        ym.atEndOfMonth()
                );

        Map<String, Integer> conteoMes = new HashMap<>();
        Map<String, Integer> conteoSemana = new HashMap<>();
        Map<Long, int[]> flujo = new HashMap<>();
        Map<String, Long> asignacionPorDiaAgente = new HashMap<>();
        Map<String, Long> asignacionPorDiaTurnoAgente = new HashMap<>();

        for (DistribucionPersonal existente : existentesMes) {
            ProgramacionTurno turno = existente.getProgramacionTurno();

            if (turnosObjetivo.contains(turno.getId())) {
                continue;
            }

            Long trabajadorId = turno.getTrabajador().getId();
            Long ubicacionId = existente.getUbicacion().getId();

            incrementar(conteoMes, key(trabajadorId, ubicacionId));
            incrementar(
                    conteoSemana,
                    keySemana(
                            trabajadorId,
                            ubicacionId,
                            semanaDelMes(turno.getFecha())
                    )
            );

            GrupoFlujoCaseta grupo =
                    grupo(configCaseta.get(ubicacionId));
            incrementarFlujo(flujo, trabajadorId, grupo);

            asignacionPorDiaAgente.put(
                    keyDia(trabajadorId, turno.getFecha()),
                    ubicacionId
            );

            asignacionPorDiaTurnoAgente.put(
                    keyDiaTurno(
                            trabajadorId,
                            turno.getFecha(),
                            turno.getEstado()
                    ),
                    ubicacionId
            );
        }

        /*
         * Para los primeros días del mes también necesitamos conocer
         * las asignaciones de los dos días previos, aunque pertenezcan
         * al mes anterior.
         */
        for (DistribucionPersonal anterior :
                distribucionRepository.findMes(
                        plazaId,
                        ym.atDay(1).minusDays(2),
                        ym.atDay(1).minusDays(1)
                )) {
            ProgramacionTurno turnoAnterior =
                    anterior.getProgramacionTurno();

            if (!turnoAnterior.getEstado().esOperativo()) {
                continue;
            }

            asignacionPorDiaTurnoAgente.put(
                    keyDiaTurno(
                            turnoAnterior.getTrabajador().getId(),
                            turnoAnterior.getFecha(),
                            turnoAnterior.getEstado()
                    ),
                    anterior.getUbicacion().getId()
            );

            asignacionPorDiaAgente.put(
                    keyDia(
                            turnoAnterior.getTrabajador().getId(),
                            turnoAnterior.getFecha()
                    ),
                    anterior.getUbicacion().getId()
            );
        }

        /*
         * Multi-start Greedy + Backtracking.
         *
         * Cada ejecución completa puede encontrar una combinación distinta
         * porque el solver mezcla agentes/candidatos. Repetimos el periodo
         * completo y conservamos la mejor solución global, no solo la mejor
         * solución local de cada día/turno.
         *
         * Objetivo lexicográfico:
         * 1) cero observaciones siempre que exista una solución factible;
         * 2) máxima cobertura de las casetas normales del turno;
         * 3) mayor cantidad de asignaciones;
         * 4) menor puntaje total.
         */
        IntentoGeneracion mejorIntento = null;
        Set<Long> trabajadoresPrioritarios = Set.of();

        final int maxIntentos = 2;

        for (int intento = 0;
                intento < maxIntentos;
                intento++) {
            /*
             * Primera pasada: respeta todas las preferencias de rotación.
             * Segunda pasada: mantiene las reglas estructurales (flujo,
             * jerarquía, no duplicidad), pero permite superar topes
             * históricos de semana/mes para no dejar personal sin caseta.
             */
            boolean flujoEstricto =
                    intento == 0;

            IntentoGeneracion candidato =
                    ejecutarIntento(
                            turnos,
                            trabajadoresPrioritarios,
                            ubicaciones,
                            configCaseta,
                            restricciones,
                            config,
                            conteoMes,
                            conteoSemana,
                            flujo,
                            asignacionPorDiaAgente,
                            asignacionPorDiaTurnoAgente,
                            flujoEstricto
                    );

            if (mejorIntento == null
                    || esMejorIntento(
                            candidato,
                            mejorIntento,
                            turnos,
                            ubicaciones
                    )) {
                mejorIntento =
                        candidato;
            }

            if (candidato.conflictos().isEmpty()
                    && deficitCoberturaNormal(
                            candidato,
                            turnos,
                            ubicaciones
                    ) == 0) {
                mejorIntento =
                        candidato;
                break;
            }

            trabajadoresPrioritarios =
                    candidato.conflictos()
                            .stream()
                            .map(
                                    Conflicto::trabajadorId
                            )
                            .collect(
                                    Collectors.toSet()
                            );
        }

        if (mejorIntento == null) {
            throw new ProgramacionValidationException(
                    "No fue posible construir una propuesta de distribución"
            );
        }

        validarSinDuplicidadDeCaseta(
                mejorIntento.asignaciones()
        );

        return new Propuesta(
                plazaId,
                anio,
                mes,
                periodo,
                semana,
                desde,
                hasta,
                mejorIntento.asignaciones(),
                mejorIntento.conflictos()
        );
    }

    private void validarSinDuplicidadDeCaseta(
            List<ItemPropuesta> asignaciones
    ) {
        Set<String> ocupadas =
                new HashSet<>();

        for (ItemPropuesta item :
                asignaciones) {
            String clave =
                    item.fecha()
                            + "|"
                            + item.turno()
                            + "|"
                            + item.ubicacionId();

            if (!ocupadas.add(
                    clave
            )) {
                throw new ProgramacionValidationException(
                        "El generador intentó asignar la caseta "
                                + item.ubicacionCodigo()
                                + " a más de una persona en el turno "
                                + item.turno()
                                + " del "
                                + item.fecha()
                );
            }
        }
    }

    private IntentoGeneracion ejecutarIntento(
            List<ProgramacionTurno> turnosBase,
            Set<Long> trabajadoresPrioritarios,
            List<ProgramacionUbicacion> ubicaciones,
            Map<Long, ConfiguracionCaseta> configCaseta,
            Set<String> restricciones,
            Configuracion config,
            Map<String, Integer> conteoMesBase,
            Map<String, Integer> conteoSemanaBase,
            Map<Long, int[]> flujoBase,
            Map<String, Long> asignacionPorDiaAgenteBase,
            Map<String, Long> asignacionPorDiaTurnoAgenteBase,
            boolean flujoEstricto
    ) {
        List<ProgramacionTurno> turnos =
                new ArrayList<>(turnosBase);

        Collections.shuffle(turnos);
        turnos.sort(
                Comparator.comparing(
                                ProgramacionTurno::getFecha
                        )
                        .thenComparing(
                                item ->
                                        item.getEstado().name()
                        )
        );

        Map<String, List<ProgramacionTurno>> gruposMap =
                new LinkedHashMap<>();

        for (ProgramacionTurno turno : turnos) {
            String clave =
                    turno.getFecha()
                            + "|"
                            + turno.getEstado().name();

            gruposMap.computeIfAbsent(
                    clave,
                    ignored -> new ArrayList<>()
            ).add(turno);
        }

        List<List<ProgramacionTurno>> grupos =
                new ArrayList<>(
                        gruposMap.values()
                );

        EstadoGeneracion estadoInicial =
                new EstadoGeneracion(
                        new HashMap<>(
                                conteoMesBase
                        ),
                        new HashMap<>(
                                conteoSemanaBase
                        ),
                        copiarFlujo(
                                flujoBase
                        ),
                        new HashMap<>(
                                asignacionPorDiaAgenteBase
                        ),
                        new HashMap<>(
                                asignacionPorDiaTurnoAgenteBase
                        ),
                        new ArrayList<>(),
                        new ArrayList<>()
                );

        /*
         * Backtracking global entre días/turnos.
         *
         * Antes el backtracking optimizaba cada día/turno por separado.
         * Eso podía elegir una combinación perfecta hoy que dejara a un
         * agente sin opciones mañana. Ahora se conservan varias soluciones
         * locales y el solver puede retroceder a grupos anteriores cuando
         * una elección genera conflictos posteriores.
         */
        BusquedaPeriodo busqueda =
                new BusquedaPeriodo(
                        flujoEstricto
                                ? 1_200
                                : 6_000,
                        flujoEstricto
                                ? 1_000L
                                : 2_800L
                );

        backtrackingPeriodo(
                grupos,
                0,
                trabajadoresPrioritarios,
                ubicaciones,
                configCaseta,
                restricciones,
                config,
                flujoEstricto,
                estadoInicial,
                busqueda,
                turnos
        );

        if (busqueda.mejor != null) {
            return busqueda.mejor;
        }

        return new IntentoGeneracion(
                estadoInicial.asignaciones,
                estadoInicial.conflictos
        );
    }

    private void backtrackingPeriodo(
            List<List<ProgramacionTurno>> grupos,
            int indice,
            Set<Long> trabajadoresPrioritarios,
            List<ProgramacionUbicacion> ubicaciones,
            Map<Long, ConfiguracionCaseta> configCaseta,
            Set<String> restricciones,
            Configuracion config,
            boolean flujoEstricto,
            EstadoGeneracion estado,
            BusquedaPeriodo busqueda,
            List<ProgramacionTurno> turnos
    ) {
        if (busqueda.perfecto) {
            return;
        }

        if (System.nanoTime()
                >= busqueda.deadlineNanos) {
            return;
        }

        if (++busqueda.nodos
                > busqueda.maxNodos) {
            return;
        }

        if (busqueda.mejor != null) {
            int conflictosActuales =
                    estado.conflictos.size();

            int conflictosMejor =
                    busqueda.mejor
                            .conflictos()
                            .size();

            if (conflictosActuales
                    > conflictosMejor) {
                return;
            }

            if (conflictosActuales
                    == conflictosMejor) {
                int turnosRestantes =
                        turnosRestantes(
                                grupos,
                                indice
                        );

                if (estado.asignaciones.size()
                        + turnosRestantes
                        < busqueda.mejor
                                .asignaciones()
                                .size()) {
                    return;
                }
            }
        }

        if (indice
                >= grupos.size()) {
            IntentoGeneracion candidato =
                    new IntentoGeneracion(
                            List.copyOf(
                                    estado.asignaciones
                            ),
                            List.copyOf(
                                    estado.conflictos
                            )
                    );

            if (busqueda.mejor == null
                    || esMejorIntento(
                            candidato,
                            busqueda.mejor,
                            turnos,
                            ubicaciones
                    )) {
                busqueda.mejor =
                        candidato;
            }

            if (candidato.conflictos()
                    .isEmpty()
                    && deficitCoberturaNormal(
                            candidato,
                            turnos,
                            ubicaciones
                    ) == 0) {
                busqueda.perfecto =
                        true;
            }

            return;
        }

        List<ProgramacionTurno> grupoTurno =
                grupos.get(
                        indice
                );

        int casetasHabilitadasTurno =
                (int) ubicaciones.stream()
                        .filter(item ->
                                permiteTurno(
                                        item,
                                        grupoTurno
                                                .get(0)
                                                .getEstado()
                                )
                        )
                        .count();

        int maxOverflowTurno =
                Math.max(
                        0,
                        grupoTurno.size()
                                - casetasHabilitadasTurno
                );

        List<ResultadoGrupoBacktracking> variantes =
                generarVariantesGrupo(
                        grupoTurno,
                        trabajadoresPrioritarios,
                        ubicaciones,
                        configCaseta,
                        restricciones,
                        config,
                        estado,
                        flujoEstricto,
                        maxOverflowTurno
                );

        for (ResultadoGrupoBacktracking variante :
                variantes) {
            EstadoGeneracion siguiente =
                    aplicarResultadoGrupo(
                            estado,
                            grupoTurno,
                            variante
                    );

            backtrackingPeriodo(
                    grupos,
                    indice + 1,
                    trabajadoresPrioritarios,
                    ubicaciones,
                    configCaseta,
                    restricciones,
                    config,
                    flujoEstricto,
                    siguiente,
                    busqueda,
                    turnos
            );

            if (busqueda.perfecto) {
                return;
            }
        }
    }

    private List<ResultadoGrupoBacktracking> generarVariantesGrupo(
            List<ProgramacionTurno> grupoTurno,
            Set<Long> trabajadoresPrioritarios,
            List<ProgramacionUbicacion> ubicaciones,
            Map<Long, ConfiguracionCaseta> configCaseta,
            Set<String> restricciones,
            Configuracion config,
            EstadoGeneracion estado,
            boolean flujoEstricto,
            int maxOverflowTurno
    ) {
        boolean contieneTrabajadorPrioritario =
                grupoTurno.stream()
                        .anyMatch(turno ->
                                trabajadoresPrioritarios.contains(
                                        turno.getTrabajador().getId()
                                )
                        );

        /*
         * Conservamos diversidad controlada en TODOS los grupos.
         *
         * Antes los grupos sin conflictos previos generaban una sola
         * solución. Eso convertía el "backtracking global" en una ruta
         * casi greedy: cuando un día posterior quedaba sin caseta, no
         * existía una alternativa anterior a la cual retroceder.
         *
         * Ahora mantenemos pocas variantes por grupo y aumentamos la
         * diversidad solo donde ya detectamos trabajadores conflictivos.
         * Así evitamos la explosión combinatoria pero permitimos reparar
         * decisiones de días anteriores.
         */
        int objetivoVariantes;

        if (contieneTrabajadorPrioritario) {
            objetivoVariantes =
                    grupoTurno.size() <= 5
                            ? 5
                            : 4;
        } else {
            objetivoVariantes =
                    grupoTurno.size() <= 5
                            ? 3
                            : 2;
        }

        int maxIntentos =
                objetivoVariantes * 3;

        Map<String, ResultadoGrupoBacktracking> unicas =
                new LinkedHashMap<>();

        for (int intento = 0;
                intento < maxIntentos
                        && unicas.size()
                                < objetivoVariantes;
                intento++) {
            ResultadoGrupoBacktracking resultado =
                    resolverGrupoBacktracking(
                            grupoTurno,
                            trabajadoresPrioritarios,
                            ubicaciones,
                            configCaseta,
                            restricciones,
                            config,
                            estado.conteoMes,
                            estado.conteoSemana,
                            estado.flujo,
                            estado.asignacionPorDiaAgente,
                            estado.asignacionPorDiaTurnoAgente,
                            flujoEstricto,
                            maxOverflowTurno
                    );

            unicas.putIfAbsent(
                    firmaResultadoGrupo(
                            resultado
                    ),
                    resultado
            );
        }

        if (unicas.isEmpty()) {
            return List.of(
                    new ResultadoGrupoBacktracking(
                            Map.of(),
                            Integer.MAX_VALUE
                    )
            );
        }

        return unicas.values()
                .stream()
                .sorted(
                        Comparator
                                .comparingInt(
                                        (
                                                ResultadoGrupoBacktracking item
                                        ) ->
                                                item.asignaciones()
                                                        .size()
                                )
                                .reversed()
                                .thenComparingInt(
                                        ResultadoGrupoBacktracking::puntaje
                                )
                )
                .toList();
    }

    private String firmaResultadoGrupo(
            ResultadoGrupoBacktracking resultado
    ) {
        return resultado.asignaciones()
                .entrySet()
                .stream()
                .sorted(
                        Map.Entry.comparingByKey()
                )
                .map(entry ->
                        entry.getKey()
                                + ":"
                                + entry.getValue()
                                        .ubicacion()
                                        .getId()
                )
                .collect(
                        Collectors.joining(
                                "|"
                        )
                );
    }

    private EstadoGeneracion aplicarResultadoGrupo(
            EstadoGeneracion base,
            List<ProgramacionTurno> grupoTurno,
            ResultadoGrupoBacktracking resultado
    ) {
        EstadoGeneracion siguiente =
                copiarEstado(
                        base
                );

        /*
         * Defensa adicional contra duplicados.
         * Aunque el backtracking ya mantiene un Set de casetas ocupadas,
         * volvemos a comprobar aquí antes de materializar cada asignación.
         */
        Set<Long> casetasOcupadasGrupo =
                siguiente.asignaciones.stream()
                        .filter(item ->
                                !grupoTurno.isEmpty()
                                        && Objects.equals(
                                                item.fecha(),
                                                grupoTurno.get(0).getFecha()
                                        )
                                        && Objects.equals(
                                                item.turno(),
                                                grupoTurno.get(0)
                                                        .getEstado()
                                                        .name()
                                        )
                        )
                        .map(
                                ItemPropuesta::ubicacionId
                        )
                        .collect(
                                Collectors.toSet()
                        );

        for (ProgramacionTurno turno :
                grupoTurno) {
            Candidate elegido =
                    resultado.asignaciones()
                            .get(
                                    turno.getId()
                            );

            if (elegido == null) {
                siguiente.conflictos.add(
                        new Conflicto(
                                turno.getId(),
                                turno.getTrabajador().getId(),
                                turno.getTrabajador()
                                        .getNombreCompleto(),
                                turno.getFecha(),
                                turno.getEstado().name(),
                                "No se encontró una combinación válida para cubrir este agente sin romper las restricciones"
                        )
                );
                continue;
            }

            Long trabajadorId =
                    turno.getTrabajador().getId();

            ProgramacionUbicacion ubicacion =
                    elegido.ubicacion();

            if (!casetasOcupadasGrupo.add(
                    ubicacion.getId()
            )) {
                throw new ProgramacionValidationException(
                        "No se permite asignar la caseta "
                                + ubicacion.getCodigo()
                                + " a dos agentes en el mismo turno "
                                + turno.getEstado().name()
                                + " del "
                                + turno.getFecha()
                );
            }

            siguiente.asignaciones.add(
                    new ItemPropuesta(
                            turno.getId(),
                            trabajadorId,
                            turno.getTrabajador().getCodigo(),
                            turno.getTrabajador()
                                    .getNombreCompleto(),
                            turno.getFecha(),
                            turno.getEstado().name(),
                            ubicacion.getId(),
                            ubicacion.getCodigo(),
                            ubicacion.getNombre(),
                            elegido.grupo(),
                            elegido.score()
                    )
            );

            incrementar(
                    siguiente.conteoMes,
                    key(
                            trabajadorId,
                            ubicacion.getId()
                    )
            );

            incrementar(
                    siguiente.conteoSemana,
                    keySemana(
                            trabajadorId,
                            ubicacion.getId(),
                            semanaDelMes(
                                    turno.getFecha()
                            )
                    )
            );

            incrementarFlujo(
                    siguiente.flujo,
                    trabajadorId,
                    elegido.grupo()
            );

            siguiente.asignacionPorDiaAgente.put(
                    keyDia(
                            trabajadorId,
                            turno.getFecha()
                    ),
                    ubicacion.getId()
            );

            siguiente.asignacionPorDiaTurnoAgente.put(
                    keyDiaTurno(
                            trabajadorId,
                            turno.getFecha(),
                            turno.getEstado()
                    ),
                    ubicacion.getId()
            );
        }

        return siguiente;
    }

    private EstadoGeneracion copiarEstado(
            EstadoGeneracion original
    ) {
        return new EstadoGeneracion(
                new HashMap<>(
                        original.conteoMes
                ),
                new HashMap<>(
                        original.conteoSemana
                ),
                copiarFlujo(
                        original.flujo
                ),
                new HashMap<>(
                        original.asignacionPorDiaAgente
                ),
                new HashMap<>(
                        original.asignacionPorDiaTurnoAgente
                ),
                new ArrayList<>(
                        original.asignaciones
                ),
                new ArrayList<>(
                        original.conflictos
                )
        );
    }

    private int turnosRestantes(
            List<List<ProgramacionTurno>> grupos,
            int desdeIndice
    ) {
        int total =
                0;

        for (int i = desdeIndice;
                i < grupos.size();
                i++) {
            total +=
                    grupos.get(i).size();
        }

        return total;
    }

    private static final class BusquedaPeriodo {

        private final long maxNodos;
        private final long deadlineNanos;
        private long nodos = 0;
        private IntentoGeneracion mejor;
        private boolean perfecto = false;

        private BusquedaPeriodo(
                long maxNodos,
                long maxMillis
        ) {
            this.maxNodos =
                    maxNodos;

            this.deadlineNanos =
                    System.nanoTime()
                            + (
                                    maxMillis
                                            * 1_000_000L
                            );
        }
    }

    private static final class EstadoGeneracion {

        private final Map<String, Integer> conteoMes;
        private final Map<String, Integer> conteoSemana;
        private final Map<Long, int[]> flujo;
        private final Map<String, Long> asignacionPorDiaAgente;
        private final Map<String, Long> asignacionPorDiaTurnoAgente;
        private final List<ItemPropuesta> asignaciones;
        private final List<Conflicto> conflictos;

        private EstadoGeneracion(
                Map<String, Integer> conteoMes,
                Map<String, Integer> conteoSemana,
                Map<Long, int[]> flujo,
                Map<String, Long> asignacionPorDiaAgente,
                Map<String, Long> asignacionPorDiaTurnoAgente,
                List<ItemPropuesta> asignaciones,
                List<Conflicto> conflictos
        ) {
            this.conteoMes =
                    conteoMes;
            this.conteoSemana =
                    conteoSemana;
            this.flujo =
                    flujo;
            this.asignacionPorDiaAgente =
                    asignacionPorDiaAgente;
            this.asignacionPorDiaTurnoAgente =
                    asignacionPorDiaTurnoAgente;
            this.asignaciones =
                    asignaciones;
            this.conflictos =
                    conflictos;
        }
    }

    private ResultadoGrupoBacktracking resolverGrupoBacktracking(
            List<ProgramacionTurno> grupoTurno,
            Set<Long> trabajadoresPrioritarios,
            List<ProgramacionUbicacion> ubicaciones,
            Map<Long, ConfiguracionCaseta> configCaseta,
            Set<String> restricciones,
            Configuracion config,
            Map<String, Integer> conteoMes,
            Map<String, Integer> conteoSemana,
            Map<Long, int[]> flujo,
            Map<String, Long> asignacionPorDiaAgente,
            Map<String, Long> asignacionPorDiaTurnoAgente,
            boolean flujoEstricto,
            int maxOverflowTurno
    ) {
        /*
         * Fase 1: Greedy inteligente.
         * Construimos rápidamente una solución inicial usando MRV
         * (agente con menos opciones primero) y el menor puntaje.
         * Esa solución sirve como incumbent para podar el backtracking.
         */
        ResultadoGrupoBacktracking semillaGreedy =
                construirSemillaGreedy(
                        grupoTurno,
                        trabajadoresPrioritarios,
                        ubicaciones,
                        configCaseta,
                        restricciones,
                        config,
                        conteoMes,
                        conteoSemana,
                        flujo,
                        asignacionPorDiaAgente,
                        asignacionPorDiaTurnoAgente,
                        flujoEstricto,
                        maxOverflowTurno
                );

        if (semillaGreedy.asignaciones().size()
                == grupoTurno.size()) {
            return semillaGreedy;
        }

        long maxNodos =
                100_000L;

        BusquedaBacktracking busqueda =
                new BusquedaBacktracking(
                        maxNodos,
                        300L,
                        semillaGreedy,
                        grupoTurno.size()
                );

        /*
         * Fase 2: Backtracking.
         * Parte desde cero, pero ya conoce una solución greedy válida.
         * Solo conserva ramas capaces de igualar o superar la cobertura
         * de esa solución y busca reducir observaciones/puntaje.
         */
        List<ProgramacionTurno> pendientes =
                new ArrayList<>(grupoTurno);

        Collections.shuffle(pendientes);

        backtrackingGrupo(
                pendientes,
                new HashMap<>(),
                new HashSet<>(),
                0,
                trabajadoresPrioritarios,
                ubicaciones,
                configCaseta,
                restricciones,
                config,
                conteoMes,
                conteoSemana,
                flujo,
                asignacionPorDiaAgente,
                asignacionPorDiaTurnoAgente,
                flujoEstricto,
                maxOverflowTurno,
                busqueda
        );

        return new ResultadoGrupoBacktracking(
                busqueda.mejorAsignacion,
                busqueda.mejorPuntaje
        );
    }

    private ResultadoGrupoBacktracking construirSemillaGreedy(
            List<ProgramacionTurno> grupoTurno,
            Set<Long> trabajadoresPrioritarios,
            List<ProgramacionUbicacion> ubicaciones,
            Map<Long, ConfiguracionCaseta> configCaseta,
            Set<String> restricciones,
            Configuracion config,
            Map<String, Integer> conteoMes,
            Map<String, Integer> conteoSemana,
            Map<Long, int[]> flujo,
            Map<String, Long> asignacionPorDiaAgente,
            Map<String, Long> asignacionPorDiaTurnoAgente,
            boolean flujoEstricto,
            int maxOverflowTurno
    ) {
        List<ProgramacionTurno> pendientes =
                new ArrayList<>(grupoTurno);

        Collections.shuffle(pendientes);

        Map<Long, Candidate> asignadas =
                new HashMap<>();

        Set<Long> ocupadas =
                new HashSet<>();

        int puntaje =
                0;

        while (!pendientes.isEmpty()) {
            ProgramacionTurno seleccionado =
                    null;

            List<Candidate> opcionesSeleccionado =
                    List.of();

            int menorCantidad =
                    Integer.MAX_VALUE;

            for (ProgramacionTurno turno :
                    pendientes) {
                List<Candidate> opciones =
                        candidatosValidos(
                                turno,
                                ocupadas,
                                ubicaciones,
                                configCaseta,
                                restricciones,
                                config,
                                conteoMes,
                                conteoSemana,
                                flujo,
                                asignacionPorDiaAgente,
                                asignacionPorDiaTurnoAgente,
                                flujoEstricto,
                                contarOverflow(
                                        asignadas.values(),
                                        turno.getEstado()
                                ),
                                maxOverflowTurno
                        );

                if (opciones.isEmpty()) {
                    continue;
                }

                boolean prioritario =
                        trabajadoresPrioritarios.contains(
                                turno.getTrabajador().getId()
                        );

                boolean seleccionadoPrioritario =
                        seleccionado != null
                                && trabajadoresPrioritarios.contains(
                                        seleccionado
                                                .getTrabajador()
                                                .getId()
                                );

                if (seleccionado == null
                        || opciones.size()
                        < menorCantidad
                        || opciones.size()
                        == menorCantidad
                        && prioritario
                        && !seleccionadoPrioritario) {
                    seleccionado =
                            turno;
                    opcionesSeleccionado =
                            opciones;
                    menorCantidad =
                            opciones.size();
                }
            }

            if (seleccionado == null) {
                break;
            }

            Candidate mejor =
                    opcionesSeleccionado.stream()
                            .min(
                                    Comparator.comparingInt(
                                            Candidate::score
                                    )
                            )
                            .orElseThrow();

            asignadas.put(
                    seleccionado.getId(),
                    mejor
            );

            ocupadas.add(
                    mejor.ubicacion().getId()
            );

            puntaje +=
                    mejor.score();

            pendientes.remove(
                    seleccionado
            );
        }

        return new ResultadoGrupoBacktracking(
                asignadas,
                puntaje
        );
    }

    private void backtrackingGrupo(
            List<ProgramacionTurno> pendientes,
            Map<Long, Candidate> actuales,
            Set<Long> ocupadas,
            int puntajeActual,
            Set<Long> trabajadoresPrioritarios,
            List<ProgramacionUbicacion> ubicaciones,
            Map<Long, ConfiguracionCaseta> configCaseta,
            Set<String> restricciones,
            Configuracion config,
            Map<String, Integer> conteoMes,
            Map<String, Integer> conteoSemana,
            Map<Long, int[]> flujo,
            Map<String, Long> asignacionPorDiaAgente,
            Map<String, Long> asignacionPorDiaTurnoAgente,
            boolean flujoEstricto,
            int maxOverflowTurno,
            BusquedaBacktracking busqueda
    ) {
        if (busqueda.solucionCompleta) {
            return;
        }

        if (System.nanoTime()
                >= busqueda.deadlineNanos) {
            actualizarMejorBacktracking(
                    actuales,
                    puntajeActual,
                    busqueda
            );
            return;
        }

        if (++busqueda.nodos
                > busqueda.maxNodos) {
            actualizarMejorBacktracking(
                    actuales,
                    puntajeActual,
                    busqueda
            );
            return;
        }

        if (actuales.size()
                + pendientes.size()
                < busqueda.mejorAsignados) {
            return;
        }

        if (pendientes.isEmpty()) {
            actualizarMejorBacktracking(
                    actuales,
                    puntajeActual,
                    busqueda
            );
            return;
        }

        /*
         * Memoización del estado combinatorio.
         *
         * Para el resto del grupo, las opciones futuras dependen de:
         * - qué trabajadores faltan;
         * - qué casetas ya están ocupadas.
         *
         * Si llegamos al mismo estado con un puntaje igual o peor,
         * esa rama está dominada y no aporta ninguna solución mejor.
         */
        String firmaEstado =
                firmaEstadoBacktracking(
                        pendientes,
                        ocupadas
                );

        Integer mejorPuntajeEstado =
                busqueda.mejorPuntajePorEstado
                        .get(
                                firmaEstado
                        );

        if (mejorPuntajeEstado != null
                && mejorPuntajeEstado <= puntajeActual) {
            return;
        }

        busqueda.mejorPuntajePorEstado.put(
                firmaEstado,
                puntajeActual
        );

        ProgramacionTurno seleccionado = null;
        List<Candidate> opcionesSeleccionado =
                List.of();

        List<ProgramacionTurno> ordenEvaluacion =
                new ArrayList<>(pendientes);

        Collections.shuffle(ordenEvaluacion);

        int menorCantidad =
                Integer.MAX_VALUE;

        for (ProgramacionTurno turno :
                ordenEvaluacion) {
            List<Candidate> opciones =
                    candidatosValidos(
                            turno,
                            ocupadas,
                            ubicaciones,
                            configCaseta,
                            restricciones,
                            config,
                            conteoMes,
                            conteoSemana,
                            flujo,
                            asignacionPorDiaAgente,
                            asignacionPorDiaTurnoAgente,
                            flujoEstricto,
                            contarOverflow(
                                    actuales.values(),
                                    turno.getEstado()
                            ),
                            maxOverflowTurno
                    );

            if (opciones.isEmpty()) {
                /*
                 * Puede ser un AUXILIAR que todavía no es elegible
                 * porque faltan vías. No lo descartamos aún.
                 */
                continue;
            }

            boolean prioritario =
                    trabajadoresPrioritarios.contains(
                            turno.getTrabajador().getId()
                    );

            boolean seleccionadoPrioritario =
                    seleccionado != null
                            && trabajadoresPrioritarios.contains(
                                    seleccionado
                                            .getTrabajador()
                                            .getId()
                            );

            if (seleccionado == null
                    || opciones.size()
                    < menorCantidad
                    || opciones.size()
                    == menorCantidad
                    && prioritario
                    && !seleccionadoPrioritario) {
                seleccionado =
                        turno;
                opcionesSeleccionado =
                        opciones;
                menorCantidad =
                        opciones.size();
            }
        }

        if (seleccionado == null) {
            actualizarMejorBacktracking(
                    actuales,
                    puntajeActual,
                    busqueda
            );
            return;
        }

        List<ProgramacionTurno> restantes =
                new ArrayList<>(pendientes);

        restantes.remove(seleccionado);

        List<Candidate> opcionesOrdenadas =
                new ArrayList<>(
                        opcionesSeleccionado
                );

        opcionesOrdenadas.sort(
                Comparator.comparingInt(
                        Candidate::score
                )
        );

        for (Candidate candidato :
                opcionesOrdenadas) {
            Long ubicacionId =
                    candidato.ubicacion().getId();

            actuales.put(
                    seleccionado.getId(),
                    candidato
            );

            ocupadas.add(
                    ubicacionId
            );

            backtrackingGrupo(
                    restantes,
                    actuales,
                    ocupadas,
                    puntajeActual
                            + candidato.score(),
                    trabajadoresPrioritarios,
                    ubicaciones,
                    configCaseta,
                    restricciones,
                    config,
                    conteoMes,
                    conteoSemana,
                    flujo,
                    asignacionPorDiaAgente,
                    asignacionPorDiaTurnoAgente,
                    flujoEstricto,
                    maxOverflowTurno,
                    busqueda
            );

            ocupadas.remove(
                    ubicacionId
            );

            actuales.remove(
                    seleccionado.getId()
            );

            if (busqueda.solucionCompleta) {
                return;
            }
        }

        /*
         * Rama de recuperación: permitimos dejar temporalmente sin
         * asignación al agente seleccionado para comprobar si eso
         * permite cubrir a más personas del grupo.
         */
        backtrackingGrupo(
                restantes,
                actuales,
                ocupadas,
                puntajeActual,
                trabajadoresPrioritarios,
                ubicaciones,
                configCaseta,
                restricciones,
                config,
                conteoMes,
                conteoSemana,
                flujo,
                asignacionPorDiaAgente,
                asignacionPorDiaTurnoAgente,
                flujoEstricto,
                maxOverflowTurno,
                busqueda
        );
    }

    private void actualizarMejorBacktracking(
            Map<Long, Candidate> actuales,
            int puntajeActual,
            BusquedaBacktracking busqueda
    ) {
        int asignados =
                actuales.size();

        if (asignados
                < busqueda.mejorAsignados) {
            return;
        }

        if (asignados
                == busqueda.mejorAsignados
                && puntajeActual
                >= busqueda.mejorPuntaje) {
            return;
        }

        busqueda.mejorAsignados =
                asignados;

        busqueda.mejorPuntaje =
                puntajeActual;

        busqueda.mejorAsignacion =
                new HashMap<>(
                        actuales
                );

        /*
         * La prioridad principal del solver es completar el turno.
         * Apenas encontramos una asignación para todos los agentes,
         * detenemos el árbol local. Seguir buscando solo para reducir
         * puntaje consumía la mayor parte del tiempo sin mejorar cobertura.
         */
        if (asignados
                == busqueda.objetivoAsignados) {
            busqueda.solucionCompleta =
                    true;
        }
    }

    private String firmaEstadoBacktracking(
            List<ProgramacionTurno> pendientes,
            Set<Long> ocupadas
    ) {
        String pendientesKey =
                pendientes.stream()
                        .map(
                                ProgramacionTurno::getId
                        )
                        .sorted()
                        .map(
                                String::valueOf
                        )
                        .collect(
                                Collectors.joining(
                                        ","
                                )
                        );

        String ocupadasKey =
                ocupadas.stream()
                        .sorted()
                        .map(
                                String::valueOf
                        )
                        .collect(
                                Collectors.joining(
                                        ","
                                )
                        );

        return pendientesKey
                + "#"
                + ocupadasKey;
    }

    private List<Candidate> candidatosValidos(
            ProgramacionTurno turno,
            Set<Long> ocupadasTurno,
            List<ProgramacionUbicacion> ubicaciones,
            Map<Long, ConfiguracionCaseta> configCaseta,
            Set<String> restricciones,
            Configuracion config,
            Map<String, Integer> conteoMes,
            Map<String, Integer> conteoSemana,
            Map<Long, int[]> flujo,
            Map<String, Long> asignacionPorDiaAgente,
            Map<String, Long> asignacionPorDiaTurnoAgente,
            boolean flujoEstricto,
            int overflowUsadas,
            int maxOverflowTurno
    ) {
        Long trabajadorId =
                turno.getTrabajador().getId();

        boolean turnoC =
                turno.getEstado()
                        == EstadoProgramacion.C;

        int semanaActual =
                semanaDelMes(
                        turno.getFecha()
                );

        Long ubicacionDiaAnterior =
                asignacionPorDiaAgente
                        .get(
                                keyDia(
                                        trabajadorId,
                                        turno.getFecha()
                                                .minusDays(1)
                                )
                        );

        ProgramacionUbicacion ubicacionAnterior =
                ubicacionPorId(
                        ubicaciones,
                        ubicacionDiaAnterior
                );

        List<ProgramacionUbicacion> ubicacionesAleatorias =
                new ArrayList<>(ubicaciones);

        Collections.shuffle(
                ubicacionesAleatorias
        );

        List<Candidate> candidatos =
                new ArrayList<>();

        for (ProgramacionUbicacion ubicacion :
                ubicacionesAleatorias) {
            boolean habilitadaParaTurno =
                    permiteTurno(
                            ubicacion,
                            turno.getEstado()
                    );

            /*
             * Jerarquía obligatoria de ocupación:
             *
             * 1) Primero se llenan TODAS las VÍAS habilitadas para el turno.
             * 2) Si todavía quedan agentes, se habilitan VÍAS adicionales
             *    registradas para la plaza, aunque no estén marcadas para
             *    ese turno.
             * 3) AUXILIAR/APOYO recién pueden usarse cuando ya no queda
             *    ninguna VÍA registrada libre.
             * 4) Los AUXILIARES se habilitan secuencialmente por "orden":
             *    AUX 1 -> AUX 2 -> AUX 3...
             * 5) APOYO (incluido APPMOVIL) recién se habilita cuando
             *    TODOS los auxiliares activos ya están ocupados.
             *
             * El número de orden NO participa para VIA ni APOYO. Las VÍAS
             * continúan evaluándose aleatoriamente.
             *
             * Turno C conserva su regla especial: solo utiliza VÍAS.
             */
            if (turnoC) {
                if (ubicacion.getTipo()
                        != TipoUbicacion.VIA) {
                    continue;
                }

                if (!habilitadaParaTurno
                        && !viasHabilitadasCompletas(
                                ubicaciones,
                                turno.getEstado(),
                                ocupadasTurno
                        )) {
                    continue;
                }
            } else if (ubicacion.getTipo()
                    == TipoUbicacion.VIA) {
                if (!habilitadaParaTurno
                        && !viasHabilitadasCompletas(
                                ubicaciones,
                                turno.getEstado(),
                                ocupadasTurno
                        )) {
                    continue;
                }
            } else if (!todasLasViasCompletas(
                    ubicaciones,
                    ocupadasTurno
            )) {
                continue;
            }

            /*
             * Los AUXILIARES sí respetan el orden configurado:
             * AUX 1 debe ocuparse antes que AUX 2, AUX 2 antes que AUX 3,
             * etc. Esta regla usa "orden" únicamente para AUXILIAR.
             * VIA y APOYO continúan sin prioridad por número de orden.
             */
            if (ubicacion.getTipo()
                    == TipoUbicacion.AUXILIAR
                    && !auxiliaresPreviosCompletos(
                            ubicacion,
                            ubicaciones,
                            ocupadasTurno
                    )) {
                continue;
            }

            /*
             * APOYO es el último nivel de la jerarquía.
             * No puede entrar APPMOVIL ni ningún otro APOYO mientras
             * exista un AUXILIAR activo sin ocupar.
             */
            if (ubicacion.getTipo()
                    == TipoUbicacion.APOYO
                    && !todosLosAuxiliaresCompletos(
                            ubicaciones,
                            ocupadasTurno
                    )) {
                continue;
            }

            if (restricciones.contains(
                    key(
                            trabajadorId,
                            ubicacion.getId()
                    )
            )) {
                continue;
            }

            if (ocupadasTurno.contains(
                    ubicacion.getId()
            )) {
                continue;
            }

            ConfiguracionCaseta especifica =
                    configCaseta.get(
                            ubicacion.getId()
                    );

            int maxSemana =
                    especifica != null
                            && especifica.getMaxSemana() != null
                            ? especifica.getMaxSemana()
                            : config.maxMismaCasetaSemana();

            int maxMes =
                    especifica != null
                            && especifica.getMaxMes() != null
                            ? especifica.getMaxMes()
                            : config.maxMismaCasetaMes();

            int vecesSemana =
                    conteoSemana.getOrDefault(
                            keySemana(
                                    trabajadorId,
                                    ubicacion.getId(),
                                    semanaActual
                            ),
                            0
                    );

            int vecesMes =
                    conteoMes.getOrDefault(
                            key(
                                    trabajadorId,
                                    ubicacion.getId()
                            ),
                            0
                    );

            /*
             * En C hay pocas vías y todas son del mismo flujo. Los topes
             * semana/mes no deben dejar personas sin caseta; la rotación
             * se resuelve evitando la vía del día anterior y usando azar.
             */
            if (!turnoC
                    && flujoEstricto
                    && (
                            vecesSemana >= maxSemana
                                    || vecesMes >= maxMes
                    )) {
                continue;
            }

            int consecutivos =
                    consecutivosAnteriores(
                            trabajadorId,
                            ubicacion.getId(),
                            turno.getFecha(),
                            asignacionPorDiaAgente
                    );

            if (!turnoC
                    && flujoEstricto
                    && consecutivos
                    >= config.maxConsecutivos()) {
                continue;
            }

            Long ubicacionMismoTurnoDiaAnterior =
                    asignacionPorDiaTurnoAgente
                            .get(
                                    keyDiaTurno(
                                            trabajadorId,
                                            turno.getFecha()
                                                    .minusDays(1),
                                            turno.getEstado()
                                    )
                            );

            if (Objects.equals(
                    ubicacionMismoTurnoDiaAnterior,
                    ubicacion.getId()
            )) {
                continue;
            }

            /*
             * Regla específica de C: aunque ayer haya sido A, B o C,
             * no se repite exactamente la misma VÍA al día siguiente.
             */
            if (turnoC
                    && ubicacion.getTipo()
                    == TipoUbicacion.VIA
                    && Objects.equals(
                            ubicacionDiaAnterior,
                            ubicacion.getId()
                    )) {
                continue;
            }

            if (!turnoC
                    && flujoEstricto) {
                Long ubicacionAyer =
                        ubicacionDiaAnterior;

                Long ubicacionAnteayer =
                        asignacionPorDiaAgente
                                .get(
                                        keyDia(
                                                trabajadorId,
                                                turno.getFecha()
                                                        .minusDays(2)
                                        )
                                );

                if (Objects.equals(
                        ubicacionAyer,
                        ubicacion.getId()
                ) || Objects.equals(
                        ubicacionAnteayer,
                        ubicacion.getId()
                )) {
                    continue;
                }
            }

            GrupoFlujoCaseta grupo =
                    grupo(especifica);

            GrupoFlujoCaseta grupoDiaAnterior =
                    grupoDeUbicacion(
                            ubicacionDiaAnterior,
                            configCaseta
                    );

            /*
             * Alternancia de flujo para A/B:
             *
             * 1) En la pasada estricta primero intentamos SIEMPRE cambiar
             *    de flujo respecto al día anterior.
             * 2) Si esa combinación no es posible, la pasada flexible
             *    puede repetir el mismo flujo una sola vez, pero cambiando
             *    el tipo de ubicación respecto al día anterior.
             * 3) Nunca permitimos tres días consecutivos con el mismo flujo.
             *
             * Ejemplo válido:
             * BAJO/VIA -> ALTO/VIA -> ALTO/AUXILIAR -> BAJO/VIA
             *
             * Turno C conserva su tratamiento especial porque sus vías
             * operativas pueden pertenecer todas al mismo flujo; en C la
             * rotación se controla por vía, no por grupo ALTO/BAJO.
             */
            if (!turnoC
                    && grupo
                    != GrupoFlujoCaseta.SIN_CLASIFICAR
                    && grupoDiaAnterior
                    != GrupoFlujoCaseta.SIN_CLASIFICAR
                    && grupo == grupoDiaAnterior) {
                GrupoFlujoCaseta grupoHaceDosDias =
                        grupoDeUbicacion(
                                asignacionPorDiaAgente
                                        .get(
                                                keyDia(
                                                        trabajadorId,
                                                        turno.getFecha()
                                                                .minusDays(2)
                                                )
                                        ),
                                configCaseta
                        );

                boolean seriaTercerDiaMismoFlujo =
                        grupoHaceDosDias
                                != GrupoFlujoCaseta.SIN_CLASIFICAR
                                && grupoHaceDosDias == grupo;

                boolean repiteTipoUbicacion =
                        ubicacionAnterior != null
                                && ubicacionAnterior.getTipo()
                                == ubicacion.getTipo();

                /*
                 * Excepción de alternancia:
                 * - La pasada estricta exige ALTO -> BAJO -> ALTO...
                 * - La pasada flexible admite ALTO -> ALTO o BAJO -> BAJO.
                 * - Nunca se permiten tres días seguidos del mismo flujo.
                 * - Si repetimos flujo, debe cambiar el tipo de ubicación:
                 *   VIA/AUXILIAR/APOYO no puede ser igual al día anterior.
                 */
                if (flujoEstricto
                        || seriaTercerDiaMismoFlujo
                        || repiteTipoUbicacion) {
                    continue;
                }
            }

            int score;

            if (turnoC) {
                /*
                 * C se reparte aleatoriamente entre las vías válidas.
                 * El overflow mantiene una penalización alta, pero además
                 * está bloqueado hasta ocupar primero todas las vías
                 * habilitadas para C.
                 */
                score =
                        penalizacionOverflowTurno(
                                habilitadaParaTurno
                        )
                                ;
            } else {
                score =
                        penalizacionRepeticionCaseta(
                                vecesMes,
                                vecesSemana,
                                consecutivos,
                                false
                        )
                                + penalizacionAlternanciaFlujo(
                                        grupoDiaAnterior,
                                        grupo,
                                        false,
                                        config.balancearFlujo()
                                )
                                + penalizacionRecuperacionFlujo(
                                        flujoEstricto,
                                        false,
                                        grupoDiaAnterior,
                                        grupo
                                )
                                + penalizacionFlujo(
                                        flujo,
                                        trabajadorId,
                                        grupo,
                                        config.balancearFlujo()
                                )
                                + penalizacionOverflowTurno(
                                        habilitadaParaTurno
                                )
                                ;
            }

            candidatos.add(
                    new Candidate(
                            ubicacion,
                            grupo,
                            score
                    )
            );
        }

        return candidatos;
    }

    private boolean ubicacionesHabilitadasCompletas(
            List<ProgramacionUbicacion> ubicaciones,
            EstadoProgramacion turno,
            Set<Long> ocupadas
    ) {
        return ubicaciones.stream()
                .filter(item ->
                        Boolean.TRUE.equals(
                                item.getActivo()
                        )
                )
                .filter(item ->
                        permiteTurno(
                                item,
                                turno
                        )
                )
                .allMatch(item ->
                        ocupadas.contains(
                                item.getId()
                        )
                );
    }

    private boolean viasHabilitadasCompletas(
            List<ProgramacionUbicacion> ubicaciones,
            EstadoProgramacion turno,
            Set<Long> ocupadas
    ) {
        return ubicaciones.stream()
                .filter(item ->
                        item.getTipo()
                                == TipoUbicacion.VIA
                )
                .filter(item ->
                        Boolean.TRUE.equals(
                                item.getActivo()
                        )
                )
                .filter(item ->
                        permiteTurno(
                                item,
                                turno
                        )
                )
                .allMatch(item ->
                        ocupadas.contains(
                                item.getId()
                        )
                );
    }

    private static final class BusquedaBacktracking {

        private final long maxNodos;
        private final long deadlineNanos;
        private final int objetivoAsignados;
        private final Map<String, Integer> mejorPuntajePorEstado =
                new HashMap<>();
        private long nodos = 0;
        private int mejorAsignados;
        private int mejorPuntaje;
        private Map<Long, Candidate> mejorAsignacion;
        private boolean solucionCompleta = false;

        private BusquedaBacktracking(
                long maxNodos,
                long maxMillis,
                ResultadoGrupoBacktracking semillaGreedy,
                int objetivoAsignados
        ) {
            this.maxNodos =
                    maxNodos;

            this.deadlineNanos =
                    System.nanoTime()
                            + (
                                    maxMillis
                                            * 1_000_000L
                            );

            this.objetivoAsignados =
                    objetivoAsignados;

            this.mejorAsignacion =
                    new HashMap<>(
                            semillaGreedy.asignaciones()
                    );

            this.mejorAsignados =
                    this.mejorAsignacion.size();

            this.mejorPuntaje =
                    semillaGreedy.puntaje();

            this.solucionCompleta =
                    this.mejorAsignados
                            == objetivoAsignados;
        }
    }

    private record ResultadoGrupoBacktracking(
            Map<Long, Candidate> asignaciones,
            int puntaje
    ) {
    }

    private boolean esMejorIntento(
            IntentoGeneracion candidato,
            IntentoGeneracion actual,
            List<ProgramacionTurno> turnos,
            List<ProgramacionUbicacion> ubicaciones
    ) {
        if (candidato.conflictos().size()
                != actual.conflictos().size()) {
            return candidato.conflictos().size()
                    < actual.conflictos().size();
        }

        int deficitCandidato =
                deficitCoberturaNormal(
                        candidato,
                        turnos,
                        ubicaciones
                );

        int deficitActual =
                deficitCoberturaNormal(
                        actual,
                        turnos,
                        ubicaciones
                );

        if (deficitCandidato
                != deficitActual) {
            return deficitCandidato
                    < deficitActual;
        }

        if (candidato.asignaciones().size()
                != actual.asignaciones().size()) {
            return candidato.asignaciones().size()
                    > actual.asignaciones().size();
        }

        return puntajeTotal(
                candidato.asignaciones()
        ) < puntajeTotal(
                actual.asignaciones()
        );
    }

    private int deficitCoberturaNormal(
            IntentoGeneracion intento,
            List<ProgramacionTurno> turnos,
            List<ProgramacionUbicacion> ubicaciones
    ) {
        Map<Long, ProgramacionTurno> turnoPorId =
                turnos.stream()
                        .collect(
                                Collectors.toMap(
                                        ProgramacionTurno::getId,
                                        Function.identity()
                                )
                        );

        Map<Long, ProgramacionUbicacion> ubicacionPorId =
                ubicaciones.stream()
                        .collect(
                                Collectors.toMap(
                                        ProgramacionUbicacion::getId,
                                        Function.identity()
                                )
                        );

        Map<String, Integer> agentesPorGrupo =
                new HashMap<>();

        for (ProgramacionTurno turno :
                turnos) {
            agentesPorGrupo.merge(
                    turno.getFecha()
                            + "|"
                            + turno.getEstado().name(),
                    1,
                    Integer::sum
            );
        }

        Map<String, Set<Long>> casetasNormalesCubiertas =
                new HashMap<>();

        for (ItemPropuesta item :
                intento.asignaciones()) {
            ProgramacionTurno turno =
                    turnoPorId.get(
                            item.programacionTurnoId()
                    );

            ProgramacionUbicacion ubicacion =
                    ubicacionPorId.get(
                            item.ubicacionId()
                    );

            if (turno == null
                    || ubicacion == null
                    || ubicacion.getTipo()
                    != TipoUbicacion.VIA
                    || !permiteTurno(
                            ubicacion,
                            turno.getEstado()
                    )) {
                continue;
            }

            String key =
                    turno.getFecha()
                            + "|"
                            + turno.getEstado().name();

            casetasNormalesCubiertas
                    .computeIfAbsent(
                            key,
                            ignored -> new HashSet<>()
                    )
                    .add(
                            ubicacion.getId()
                    );
        }

        int deficit =
                0;

        for (Map.Entry<String, Integer> entry :
                agentesPorGrupo.entrySet()) {
            String[] partes =
                    entry.getKey()
                            .split("\\|");

            EstadoProgramacion estado =
                    EstadoProgramacion.valueOf(
                            partes[1]
                    );

            int casetasNormales =
                    (int) ubicaciones.stream()
                            .filter(item ->
                                    item.getTipo()
                                            == TipoUbicacion.VIA
                            )
                            .filter(item ->
                                    permiteTurno(
                                            item,
                                            estado
                                    )
                            )
                            .count();

            int objetivo =
                    Math.min(
                            entry.getValue(),
                            casetasNormales
                    );

            int cubiertas =
                    casetasNormalesCubiertas
                            .getOrDefault(
                                    entry.getKey(),
                                    Set.of()
                            )
                            .size();

            deficit +=
                    Math.max(
                            0,
                            objetivo - cubiertas
                    );
        }

        return deficit;
    }

    private int puntajeTotal(
            List<ItemPropuesta> asignaciones
    ) {
        return asignaciones.stream()
                .mapToInt(
                        ItemPropuesta::puntaje
                )
                .sum();
    }

    private Map<Long, int[]> copiarFlujo(
            Map<Long, int[]> original
    ) {
        Map<Long, int[]> copia =
                new HashMap<>();

        original.forEach(
                (trabajadorId, valores) ->
                        copia.put(
                                trabajadorId,
                                Arrays.copyOf(
                                        valores,
                                        valores.length
                                )
                        )
        );

        return copia;
    }

    private record IntentoGeneracion(
            List<ItemPropuesta> asignaciones,
            List<Conflicto> conflictos
    ) {
    }

    private ProgramacionUbicacion ubicacionPorId(
            List<ProgramacionUbicacion> ubicaciones,
            Long ubicacionId
    ) {
        if (ubicacionId == null) {
            return null;
        }

        return ubicaciones.stream()
                .filter(item ->
                        Objects.equals(
                                item.getId(),
                                ubicacionId
                        )
                )
                .findFirst()
                .orElse(null);
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

    private boolean permiteTurno(
            ProgramacionUbicacion ubicacion,
            EstadoProgramacion turno
    ) {
        return switch (turno) {
            case A -> Boolean.TRUE.equals(ubicacion.getPermiteTurnoA());
            case B -> Boolean.TRUE.equals(ubicacion.getPermiteTurnoB());
            case C -> Boolean.TRUE.equals(ubicacion.getPermiteTurnoC());
            default -> false;
        };
    }

    private boolean todasLasViasCompletas(
            List<ProgramacionUbicacion> ubicaciones,
            Set<Long> ocupadas
    ) {
        return ubicaciones.stream()
                .filter(item ->
                        Boolean.TRUE.equals(
                                item.getActivo()
                        )
                )
                .filter(item ->
                        item.getTipo()
                                == TipoUbicacion.VIA
                )
                .allMatch(item ->
                        ocupadas.contains(
                                item.getId()
                        )
                );
    }

    private boolean auxiliaresPreviosCompletos(
            ProgramacionUbicacion candidata,
            List<ProgramacionUbicacion> ubicaciones,
            Set<Long> ocupadas
    ) {
        if (candidata.getTipo()
                != TipoUbicacion.AUXILIAR) {
            return true;
        }

        int ordenCandidata =
                candidata.getOrden() == null
                        ? Integer.MAX_VALUE
                        : candidata.getOrden();

        return ubicaciones.stream()
                .filter(item ->
                        Boolean.TRUE.equals(
                                item.getActivo()
                        )
                )
                .filter(item ->
                        item.getTipo()
                                == TipoUbicacion.AUXILIAR
                )
                .filter(item -> {
                    int orden =
                            item.getOrden() == null
                                    ? Integer.MAX_VALUE
                                    : item.getOrden();

                    return orden < ordenCandidata;
                })
                .allMatch(item ->
                        ocupadas.contains(
                                item.getId()
                        )
                );
    }

    private boolean todosLosAuxiliaresCompletos(
            List<ProgramacionUbicacion> ubicaciones,
            Set<Long> ocupadas
    ) {
        return ubicaciones.stream()
                .filter(item ->
                        Boolean.TRUE.equals(
                                item.getActivo()
                        )
                )
                .filter(item ->
                        item.getTipo()
                                == TipoUbicacion.AUXILIAR
                )
                .allMatch(item ->
                        ocupadas.contains(
                                item.getId()
                        )
                );
    }

    private Plaza validarPlaza(Long plazaId) {
        if (plazaId == null) {
            throw new ProgramacionValidationException(
                    "Debe seleccionar una plaza"
            );
        }

        return plazaRepository.findById(plazaId)
                .orElseThrow(() ->
                        new ProgramacionValidationException(
                                "La plaza indicada no existe"
                        )
                );
    }

    private ConfiguracionAsignacionCaseta configuracionPorDefecto(
            Long plazaId
    ) {
        ConfiguracionAsignacionCaseta config =
                new ConfiguracionAsignacionCaseta();
        config.setPlaza(validarPlaza(plazaId));
        config.setMaxMismaCasetaSemana(3);
        config.setMaxMismaCasetaMes(8);
        config.setMaxConsecutivos(2);
        config.setBalancearFlujo(true);
        return config;
    }

    private Configuracion toConfiguracion(
            ConfiguracionAsignacionCaseta entity
    ) {
        return new Configuracion(
                entity.getPlaza().getId(),
                entity.getMaxMismaCasetaSemana(),
                entity.getMaxMismaCasetaMes(),
                entity.getMaxConsecutivos(),
                Boolean.TRUE.equals(entity.getBalancearFlujo())
        );
    }

    private Restriccion toRestriccion(
            AgenteCasetaRestriccion entity
    ) {
        return new Restriccion(
                entity.getId(),
                entity.getTrabajador().getId(),
                entity.getTrabajador().getCodigo(),
                entity.getTrabajador().getNombreCompleto(),
                entity.getUbicacion().getId(),
                entity.getUbicacion().getCodigo(),
                entity.getMotivo(),
                Boolean.TRUE.equals(entity.getActivo())
        );
    }

    private GrupoFlujoCaseta grupo(ConfiguracionCaseta config) {
        return config == null || config.getGrupoFlujo() == null
                ? GrupoFlujoCaseta.SIN_CLASIFICAR
                : config.getGrupoFlujo();
    }

    private int contarOverflow(
            Collection<Candidate> asignaciones,
            EstadoProgramacion turno
    ) {
        return (int) asignaciones.stream()
                .filter(candidate ->
                        !permiteTurno(
                                candidate.ubicacion(),
                                turno
                        )
                )
                .count();
    }

    private int penalizacionOverflowTurno(
            boolean habilitadaParaTurno
    ) {
        /*
         * Solo se consideran casetas fuera del turno cuando la demanda
         * supera la capacidad habilitada. La penalización alta hace que
         * el solver use primero las casetas normales y abra una adicional
         * únicamente cuando es necesario para evitar una observación.
         */
        return habilitadaParaTurno
                ? 0
                : 2_000;
    }

    private int penalizacionRepeticionCaseta(
            int vecesMes,
            int vecesSemana,
            int consecutivos,
            boolean turnoC
    ) {
        if (turnoC) {
            return (vecesMes * 18)
                    + (vecesSemana * 30)
                    + (consecutivos * 90);
        }

        int penalizacionSemana =
                vecesSemana == 0
                        ? 0
                        : 260 + ((vecesSemana - 1) * 120);

        int penalizacionMes =
                vecesMes * 35;

        int penalizacionConsecutiva =
                consecutivos * 180;

        return penalizacionSemana
                + penalizacionMes
                + penalizacionConsecutiva;
    }

    private int penalizacionRecuperacionFlujo(
            boolean flujoEstricto,
            boolean turnoC,
            GrupoFlujoCaseta grupoAnterior,
            GrupoFlujoCaseta grupoActual
    ) {
        if (flujoEstricto
                || turnoC
                || grupoAnterior == GrupoFlujoCaseta.SIN_CLASIFICAR
                || grupoActual == GrupoFlujoCaseta.SIN_CLASIFICAR
                || grupoAnterior != grupoActual) {
            return 0;
        }

        /*
         * Segunda pasada: repetir ALTO/ALTO o BAJO/BAJO sigue siendo
         * una opción de último recurso. La penalización es muy alta,
         * pero evita dejar a un agente sin caseta cuando no existe
         * una combinación alternada posible.
         */
        return 500;
    }

    private int penalizacionAlternanciaFlujo(
            GrupoFlujoCaseta grupoAnterior,
            GrupoFlujoCaseta grupoActual,
            boolean turnoC,
            boolean balancear
    ) {
        if (!balancear
                || grupoAnterior == GrupoFlujoCaseta.SIN_CLASIFICAR
                || grupoActual == GrupoFlujoCaseta.SIN_CLASIFICAR) {
            return 0;
        }

        if (grupoAnterior == grupoActual) {
            return turnoC ? 25 : 140;
        }

        return turnoC ? -10 : -45;
    }

    private EstadoProgramacion turnoAnterior(
            Long trabajadorId,
            LocalDate fecha,
            Map<String, Long> asignacionPorDiaTurnoAgente
    ) {
        for (EstadoProgramacion estado :
                List.of(
                        EstadoProgramacion.A,
                        EstadoProgramacion.B,
                        EstadoProgramacion.C
                )) {
            if (asignacionPorDiaTurnoAgente.containsKey(
                    keyDiaTurno(
                            trabajadorId,
                            fecha,
                            estado
                    )
            )) {
                return estado;
            }
        }

        return null;
    }

    private GrupoFlujoCaseta grupoDeUbicacion(
            Long ubicacionId,
            Map<Long, ConfiguracionCaseta> configCaseta
    ) {
        if (ubicacionId == null) {
            return GrupoFlujoCaseta.SIN_CLASIFICAR;
        }

        return grupo(
                configCaseta.get(
                        ubicacionId
                )
        );
    }

    private int penalizacionFlujo(
            Map<Long, int[]> flujo,
            Long trabajadorId,
            GrupoFlujoCaseta grupo,
            boolean balancear
    ) {
        if (!balancear
                || grupo == GrupoFlujoCaseta.SIN_CLASIFICAR) {
            return 0;
        }

        int[] actual =
                flujo.getOrDefault(
                        trabajadorId,
                        new int[]{0, 0}
                );

        if (grupo == GrupoFlujoCaseta.ALTO_FLUJO) {
            return Math.max(0, actual[0] - actual[1]) * 30;
        }

        return Math.max(0, actual[1] - actual[0]) * 30;
    }

    private void incrementarFlujo(
            Map<Long, int[]> flujo,
            Long trabajadorId,
            GrupoFlujoCaseta grupo
    ) {
        int[] actual =
                flujo.computeIfAbsent(
                        trabajadorId,
                        ignored -> new int[]{0, 0}
                );

        if (grupo == GrupoFlujoCaseta.ALTO_FLUJO) {
            actual[0]++;
        } else if (grupo == GrupoFlujoCaseta.BAJO_FLUJO) {
            actual[1]++;
        }
    }

    private int consecutivosAnteriores(
            Long trabajadorId,
            Long ubicacionId,
            LocalDate fecha,
            Map<String, Long> asignaciones
    ) {
        int consecutivos = 0;
        LocalDate cursor = fecha.minusDays(1);

        while (Objects.equals(
                asignaciones.get(keyDia(trabajadorId, cursor)),
                ubicacionId
        )) {
            consecutivos++;
            cursor = cursor.minusDays(1);
        }

        return consecutivos;
    }

    private int semanaDelMes(LocalDate fecha) {
        return ((fecha.getDayOfMonth() - 1) / 7) + 1;
    }

    private void incrementar(
            Map<String, Integer> mapa,
            String key
    ) {
        mapa.merge(key, 1, Integer::sum);
    }

    private String key(Long trabajadorId, Long ubicacionId) {
        return trabajadorId + "|" + ubicacionId;
    }

    private String keySemana(
            Long trabajadorId,
            Long ubicacionId,
            int semana
    ) {
        return trabajadorId + "|" + ubicacionId + "|" + semana;
    }

    private String keyDia(
            Long trabajadorId,
            LocalDate fecha
    ) {
        return trabajadorId + "|" + fecha;
    }

    private String keyDiaTurno(
            Long trabajadorId,
            LocalDate fecha,
            EstadoProgramacion turno
    ) {
        return trabajadorId
                + "|"
                + fecha
                + "|"
                + turno.name();
    }

    private record Candidate(
            ProgramacionUbicacion ubicacion,
            GrupoFlujoCaseta grupo,
            int score
    ) {}
}
