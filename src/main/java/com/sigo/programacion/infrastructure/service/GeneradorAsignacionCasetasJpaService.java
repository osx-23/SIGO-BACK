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

        IntentoGeneracion primerIntento =
                ejecutarIntento(
                        turnos,
                        Set.of(),
                        ubicaciones,
                        configCaseta,
                        restricciones,
                        config,
                        conteoMes,
                        conteoSemana,
                        flujo,
                        asignacionPorDiaAgente,
                        asignacionPorDiaTurnoAgente,
                        true
                );

        Set<Long> trabajadoresConConflicto =
                primerIntento.conflictos()
                        .stream()
                        .map(Conflicto::trabajadorId)
                        .collect(Collectors.toSet());

        /*
         * Segunda pasada:
         * volvemos a generar desde cero, pero dentro de cada fecha/turno
         * damos prioridad a los agentes que quedaron sin caseta en la
         * primera pasada. Así se reduce el efecto del orden greedy.
         */
        IntentoGeneracion segundoIntento =
                ejecutarIntento(
                        turnos,
                        trabajadoresConConflicto,
                        ubicaciones,
                        configCaseta,
                        restricciones,
                        config,
                        conteoMes,
                        conteoSemana,
                        flujo,
                        asignacionPorDiaAgente,
                        asignacionPorDiaTurnoAgente,
                        false
                );

        IntentoGeneracion mejorIntento =
                esMejorIntento(
                        segundoIntento,
                        primerIntento
                )
                        ? segundoIntento
                        : primerIntento;

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
        Map<String, Integer> conteoMes =
                new HashMap<>(conteoMesBase);

        Map<String, Integer> conteoSemana =
                new HashMap<>(conteoSemanaBase);

        Map<Long, int[]> flujo =
                copiarFlujo(flujoBase);

        Map<String, Long> asignacionPorDiaAgente =
                new HashMap<>(
                        asignacionPorDiaAgenteBase
                );

        Map<String, Long> asignacionPorDiaTurnoAgente =
                new HashMap<>(
                        asignacionPorDiaTurnoAgenteBase
                );

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

        Map<String, List<ProgramacionTurno>> grupos =
                new LinkedHashMap<>();

        for (ProgramacionTurno turno : turnos) {
            String clave =
                    turno.getFecha()
                            + "|"
                            + turno.getEstado().name();

            grupos.computeIfAbsent(
                    clave,
                    ignored -> new ArrayList<>()
            ).add(turno);
        }

        List<ItemPropuesta> asignaciones =
                new ArrayList<>();

        List<Conflicto> conflictos =
                new ArrayList<>();

        for (List<ProgramacionTurno> grupoTurno :
                grupos.values()) {
            int casetasHabilitadasTurno =
                    (int) ubicaciones.stream()
                            .filter(item ->
                                    permiteTurno(
                                            item,
                                            grupoTurno.get(0).getEstado()
                                    )
                            )
                            .count();

            int maxOverflowTurno =
                    Math.max(
                            0,
                            grupoTurno.size()
                                    - casetasHabilitadasTurno
                    );

            ResultadoGrupoBacktracking resultado =
                    resolverGrupoBacktracking(
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
                            contarOverflow(
                                    actuales.values(),
                                    turno.getEstado()
                            ),
                            maxOverflowTurno
                    );

            for (ProgramacionTurno turno :
                    grupoTurno) {
                Candidate elegido =
                        resultado.asignaciones()
                                .get(turno.getId());

                if (elegido == null) {
                    conflictos.add(
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

                asignaciones.add(
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
                        conteoMes,
                        key(
                                trabajadorId,
                                ubicacion.getId()
                        )
                );

                incrementar(
                        conteoSemana,
                        keySemana(
                                trabajadorId,
                                ubicacion.getId(),
                                semanaDelMes(
                                        turno.getFecha()
                                )
                        )
                );

                incrementarFlujo(
                        flujo,
                        trabajadorId,
                        elegido.grupo()
                );

                asignacionPorDiaAgente.put(
                        keyDia(
                                trabajadorId,
                                turno.getFecha()
                        ),
                        ubicacion.getId()
                );

                asignacionPorDiaTurnoAgente.put(
                        keyDiaTurno(
                                trabajadorId,
                                turno.getFecha(),
                                turno.getEstado()
                        ),
                        ubicacion.getId()
                );
            }
        }

        return new IntentoGeneracion(
                asignaciones,
                conflictos
        );
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
            int overflowUsadas,
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

        long maxNodos =
                semillaGreedy.asignaciones().size()
                        == grupoTurno.size()
                        ? 80_000L
                        : 300_000L;

        BusquedaBacktracking busqueda =
                new BusquedaBacktracking(
                        maxNodos,
                        semillaGreedy
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
            int maxOverflowTurno
    ) {
        Long trabajadorId =
                turno.getTrabajador().getId();

        int semanaActual =
                semanaDelMes(
                        turno.getFecha()
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

            if (!habilitadaParaTurno
                    && (
                            maxOverflowTurno <= 0
                                    || overflowUsadas
                                    >= maxOverflowTurno
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

            if (vecesSemana >= maxSemana
                    || vecesMes >= maxMes) {
                continue;
            }

            if (!viasCompletasAntesDeAuxiliar(
                    ubicacion,
                    ubicaciones,
                    turno.getEstado(),
                    ocupadasTurno
            )) {
                continue;
            }

            if (!cumpleOrdenSecuencial(
                    ubicacion,
                    ubicaciones,
                    turno.getEstado(),
                    ocupadasTurno
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

            if (consecutivos
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

            boolean turnoC =
                    turno.getEstado()
                            == EstadoProgramacion.C;

            if (!turnoC) {
                Long ubicacionAyer =
                        asignacionPorDiaAgente
                                .get(
                                        keyDia(
                                                trabajadorId,
                                                turno.getFecha()
                                                        .minusDays(1)
                                        )
                                );

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

            Long ubicacionDiaAnterior =
                    asignacionPorDiaAgente
                            .get(
                                    keyDia(
                                            trabajadorId,
                                            turno.getFecha()
                                                    .minusDays(1)
                                    )
                            );

            GrupoFlujoCaseta grupoDiaAnterior =
                    grupoDeUbicacion(
                            ubicacionDiaAnterior,
                            configCaseta
                    );

            if (flujoEstricto
                    && !turnoC
                    && grupo != GrupoFlujoCaseta.SIN_CLASIFICAR
                    && grupoDiaAnterior != GrupoFlujoCaseta.SIN_CLASIFICAR
                    && grupo == grupoDiaAnterior) {
                continue;
            }

            int score =
                    penalizacionRepeticionCaseta(
                            vecesMes,
                            vecesSemana,
                            consecutivos,
                            turnoC
                    )
                            + penalizacionAlternanciaFlujo(
                                    grupoDiaAnterior,
                                    grupo,
                                    turnoC,
                                    config.balancearFlujo()
                            )
                            + penalizacionRecuperacionFlujo(
                                    flujoEstricto,
                                    turnoC,
                                    grupoDiaAnterior,
                                    grupo
                            )
                            + penalizacionFlujo(
                                    flujo,
                                    trabajadorId,
                                    grupo,
                                    config.balancearFlujo()
                            )
                            + Math.max(
                                    0,
                                    ubicacion.getOrden()
                            )
                            + penalizacionOverflowTurno(
                                    habilitadaParaTurno
                            )
                            + penalizacionAleatoria();

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

    private static final class BusquedaBacktracking {

        private final long maxNodos;
        private long nodos = 0;
        private int mejorAsignados;
        private int mejorPuntaje;
        private Map<Long, Candidate> mejorAsignacion;

        private BusquedaBacktracking(
                long maxNodos,
                ResultadoGrupoBacktracking semillaGreedy
        ) {
            this.maxNodos =
                    maxNodos;

            this.mejorAsignacion =
                    new HashMap<>(
                            semillaGreedy.asignaciones()
                    );

            this.mejorAsignados =
                    this.mejorAsignacion.size();

            this.mejorPuntaje =
                    semillaGreedy.puntaje();
        }
    }

    private record ResultadoGrupoBacktracking(
            Map<Long, Candidate> asignaciones,
            int puntaje
    ) {
    }

    private boolean esMejorIntento(
            IntentoGeneracion candidato,
            IntentoGeneracion actual
    ) {
        if (candidato.conflictos().size()
                != actual.conflictos().size()) {
            return candidato.conflictos().size()
                    < actual.conflictos().size();
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

    private boolean viasCompletasAntesDeAuxiliar(
            ProgramacionUbicacion candidata,
            List<ProgramacionUbicacion> ubicaciones,
            EstadoProgramacion turno,
            Set<Long> ocupadas
    ) {
        if (candidata.getTipo() != TipoUbicacion.AUXILIAR) {
            return true;
        }

        return ubicaciones.stream()
                .filter(item -> item.getTipo() == TipoUbicacion.VIA)
                .filter(item -> Boolean.TRUE.equals(item.getActivo()))
                .filter(item -> permiteTurno(item, turno))
                .allMatch(item -> ocupadas.contains(item.getId()));
    }

    private boolean cumpleOrdenSecuencial(
            ProgramacionUbicacion candidata,
            List<ProgramacionUbicacion> ubicaciones,
            EstadoProgramacion turno,
            Set<Long> ocupadas
    ) {
        if (candidata.getTipo() == TipoUbicacion.VIA) {
            return true;
        }

        int ordenCandidata =
                candidata.getOrden() == null
                        ? Integer.MAX_VALUE
                        : candidata.getOrden();

        return ubicaciones.stream()
                .filter(item -> item.getTipo() != TipoUbicacion.VIA)
                .filter(item -> permiteTurno(item, turno))
                .filter(item -> {
                    int orden =
                            item.getOrden() == null
                                    ? Integer.MAX_VALUE
                                    : item.getOrden();
                    return orden < ordenCandidata;
                })
                .allMatch(item -> ocupadas.contains(item.getId()));
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

    private int penalizacionAleatoria() {
        /*
         * Ruido pequeño para evitar patrones deterministas.
         * No supera las penalizaciones fuertes de reglas de negocio,
         * pero sí cambia la elección entre opciones similares.
         */
        return java.util.concurrent.ThreadLocalRandom
                .current()
                .nextInt(0, 26);
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
