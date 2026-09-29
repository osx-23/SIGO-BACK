package com.sigo.programacion.infrastructure.service;

import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.programacion.application.port.in.GeneradorAsignacionCasetasUseCase;
import com.sigo.programacion.application.port.in.GeneradorAsignacionCasetasUseCase.CasetaConfig;
import com.sigo.programacion.application.port.in.GeneradorAsignacionCasetasUseCase.Calidad;
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
         * Para la rotación por flujo necesitamos conocer la última vez
         * que cada agente estuvo en ALTO/BAJO, aunque haya tenido días
         * libres o el antecedente pertenezca al mes anterior.
         */
        for (DistribucionPersonal anterior :
                distribucionRepository.findMes(
                        plazaId,
                        ym.atDay(1).minusDays(31),
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

        /*
         * Guardamos también las soluciones completas de cada fase.
         * Antes se detenía la búsqueda en la PRIMERA solución completa,
         * por lo que la calidad dependía demasiado del orden aleatorio
         * obtenido en ese clic.
         */
        List<IntentoGeneracion> intentosCompletos =
                new ArrayList<>();

        Set<Long> trabajadoresPrioritarios = Set.of();

        final int maxIntentos = 3;

        for (int intento = 0;
                intento < maxIntentos;
                intento++) {
            /*
             * Fase 1: asignación normal, respeta todas las preferencias.
             * Fase 2: flexible, prioriza cobertura y permite excepciones.
             * Fase 3: reparación de cobertura. Mantiene solo las reglas
             * duras: no duplicidad, jerarquía, restricciones del agente
             * y máximo dos días consecutivos del mismo flujo.
             */
            boolean flujoEstricto =
                    intento == 0;

            boolean coberturaForzada =
                    intento == 2;

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
                            flujoEstricto,
                            coberturaForzada
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
                intentosCompletos.add(
                        candidato
                );
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

        /*
         * Optimizamos la mejor solución encontrada y, cuando ya existe
         * cobertura completa, probamos dos arranques estrictos adicionales.
         * Son intentos cortos: aportan diversidad aleatoria sin triplicar
         * el costo de las fases flexible y de recuperación.
         */
        List<IntentoGeneracion> solucionesCalidad =
                new ArrayList<>();

        Set<String> firmasCalidad =
                new HashSet<>();

        IntentoGeneracion optimizadaBase =
                optimizarIntentoPostGeneracion(
                        mejorIntento,
                        turnos,
                        ubicaciones,
                        configCaseta,
                        restricciones,
                        config,
                        asignacionPorDiaAgente
                );

        solucionesCalidad.add(
                optimizadaBase
        );

        firmasCalidad.add(
                firmaIntento(
                        optimizadaBase
                )
        );

        /*
         * Evaluamos todas las soluciones completas encontradas en las
         * fases normal/flexible/recuperación. De este modo la primera
         * generación ya compara alternativas reales en lugar de devolver
         * la primera combinación factible.
         */
        for (IntentoGeneracion completo :
                intentosCompletos) {
            IntentoGeneracion optimizado =
                    optimizarIntentoPostGeneracion(
                            completo,
                            turnos,
                            ubicaciones,
                            configCaseta,
                            restricciones,
                            config,
                            asignacionPorDiaAgente
                    );

            String firma =
                    firmaIntento(
                            optimizado
                    );

            if (firmasCalidad.add(
                    firma
            )) {
                solucionesCalidad.add(
                        optimizado
                );
            }
        }

        boolean baseCompleta =
                optimizadaBase.conflictos()
                        .isEmpty()
                        && deficitCoberturaNormal(
                                optimizadaBase,
                                turnos,
                                ubicaciones
                        ) == 0;

        if (baseCompleta) {
            /*
             * Intentos estrictos adicionales de calidad.
             * El objetivo es reunir hasta cinco soluciones distintas,
             * suficiente para reducir la dependencia del azar sin disparar
             * el tiempo del generador.
             */
            final int arranquesCalidadExtra =
                    3;

            for (int inicio = 0;
                    inicio < arranquesCalidadExtra
                            && solucionesCalidad.size() < 5;
                    inicio++) {
                IntentoGeneracion alternativo =
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
                                true,
                                false
                        );

                if (!alternativo.conflictos()
                        .isEmpty()
                        || deficitCoberturaNormal(
                                alternativo,
                                turnos,
                                ubicaciones
                        ) != 0) {
                    continue;
                }

                alternativo =
                        optimizarIntentoPostGeneracion(
                                alternativo,
                                turnos,
                                ubicaciones,
                                configCaseta,
                                restricciones,
                                config,
                                asignacionPorDiaAgente
                        );

                String firma =
                        firmaIntento(
                                alternativo
                        );

                if (firmasCalidad.add(
                        firma
                )) {
                    solucionesCalidad.add(
                            alternativo
                    );
                }
            }
        }

        int solucionesEvaluadas =
                solucionesCalidad.size();

        IntentoGeneracion elegida =
                null;

        Calidad calidadElegida =
                null;

        for (IntentoGeneracion solucion :
                solucionesCalidad) {
            int cambiosRespectoBase =
                    contarCambiosEntreIntentos(
                            optimizadaBase,
                            solucion
                    );

            Calidad calidad =
                    calcularCalidad(
                            solucion,
                            turnos,
                            ubicaciones,
                            configCaseta,
                            restricciones,
                            asignacionPorDiaAgente,
                            solucionesEvaluadas,
                            cambiosRespectoBase
                    );

            if (elegida == null
                    || esMejorCalidad(
                            calidad,
                            calidadElegida
                    )) {
                elegida =
                        solucion;
                calidadElegida =
                        calidad;
            }
        }

        mejorIntento =
                Objects.requireNonNull(
                        elegida
                );

        validarSinDuplicidadDeCaseta(
                mejorIntento.asignaciones()
        );

        validarRestriccionesFinales(
                mejorIntento.asignaciones(),
                restricciones
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
                mejorIntento.conflictos(),
                calidadElegida
        );
    }

    private IntentoGeneracion optimizarIntentoPostGeneracion(
            IntentoGeneracion intento,
            List<ProgramacionTurno> turnos,
            List<ProgramacionUbicacion> ubicaciones,
            Map<Long, ConfiguracionCaseta> configCaseta,
            Set<String> restricciones,
            Configuracion config,
            Map<String, Long> historialBase
    ) {
        IntentoGeneracion optimizado =
                repararRotacionMismoFlujo(
                        intento,
                        turnos,
                        ubicaciones,
                        configCaseta,
                        restricciones,
                        historialBase
                );

        return repararIntercambiosGlobales(
                optimizado,
                turnos,
                ubicaciones,
                configCaseta,
                restricciones,
                config,
                historialBase
        );
    }

    private String firmaIntento(
            IntentoGeneracion intento
    ) {
        return intento.asignaciones()
                .stream()
                .sorted(
                        Comparator.comparing(
                                ItemPropuesta::programacionTurnoId
                        )
                )
                .map(item ->
                        item.programacionTurnoId()
                                + ":"
                                + item.ubicacionId()
                )
                .collect(
                        Collectors.joining(
                                "|"
                        )
                );
    }

    private Calidad calcularCalidad(
            IntentoGeneracion intento,
            List<ProgramacionTurno> turnos,
            List<ProgramacionUbicacion> ubicaciones,
            Map<Long, ConfiguracionCaseta> configCaseta,
            Set<String> restricciones,
            Map<String, Long> historialBase,
            int solucionesEvaluadas,
            int cambiosRespectoBase
    ) {
        Map<Long, ProgramacionTurno> turnoPorId =
                turnos.stream()
                        .collect(
                                Collectors.toMap(
                                        ProgramacionTurno::getId,
                                        Function.identity()
                                )
                        );

        List<ItemPropuesta> ordenadas =
                intento.asignaciones()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                                ItemPropuesta::fecha
                                        )
                                        .thenComparing(
                                                ItemPropuesta::turno
                                        )
                                        .thenComparing(
                                                ItemPropuesta::trabajadorId
                                        )
                        )
                        .toList();

        Map<String, Long> historial =
                new HashMap<>(
                        historialBase
                );

        Set<String> ocupacion =
                new HashSet<>();

        int duplicidades =
                0;

        int restriccionesVioladas =
                0;

        int transicionesFlujo =
                0;

        int cambiosFlujo =
                0;

        int retornosTipo =
                0;

        int cambiosTipo =
                0;

        int repeticionesTipo =
                0;

        int transicionesCaseta =
                0;

        int cambiosCaseta =
                0;

        int repeticionesExactas =
                0;

        for (ItemPropuesta item :
                ordenadas) {
            ProgramacionTurno turno =
                    turnoPorId.get(
                            item.programacionTurnoId()
                    );

            ProgramacionUbicacion ubicacion =
                    ubicacionPorId(
                            ubicaciones,
                            item.ubicacionId()
                    );

            if (turno == null
                    || ubicacion == null) {
                continue;
            }

            String keyOcupacion =
                    item.fecha()
                            + "|"
                            + item.turno()
                            + "|"
                            + item.ubicacionId();

            if (!ocupacion.add(
                    keyOcupacion
            )) {
                duplicidades++;
            }

            if (restricciones.contains(
                    key(
                            item.trabajadorId(),
                            item.ubicacionId()
                    )
            )) {
                restriccionesVioladas++;
            }

            Long ayer =
                    historial.get(
                            keyDia(
                                    item.trabajadorId(),
                                    item.fecha()
                                            .minusDays(1)
                            )
                    );

            if (ayer != null) {
                transicionesCaseta++;

                if (Objects.equals(
                        ayer,
                        item.ubicacionId()
                )) {
                    repeticionesExactas++;
                } else {
                    cambiosCaseta++;
                }
            }

            GrupoFlujoCaseta grupoActual =
                    grupoDeUbicacion(
                            item.ubicacionId(),
                            configCaseta
                    );

            if (turno.getEstado()
                    != EstadoProgramacion.C) {
                GrupoFlujoCaseta grupoAyer =
                        grupoDeUbicacion(
                                ayer,
                                configCaseta
                        );

                if (ayer != null
                        && grupoActual
                        != GrupoFlujoCaseta.SIN_CLASIFICAR
                        && grupoAyer
                        != GrupoFlujoCaseta.SIN_CLASIFICAR) {
                    transicionesFlujo++;

                    if (grupoActual
                            != grupoAyer) {
                        cambiosFlujo++;
                    }
                }

                if (flujoPermiteAlternanciaTipo(
                        grupoActual,
                        turno.getEstado(),
                        ubicaciones,
                        configCaseta
                )) {
                    ProgramacionUbicacion ultimaMismoFlujo =
                            ultimaUbicacionEnMismoFlujo(
                                    item.trabajadorId(),
                                    item.fecha(),
                                    grupoActual,
                                    historial,
                                    ubicaciones,
                                    configCaseta
                            );

                    if (ultimaMismoFlujo != null
                            && esViaOAuxiliar(
                                    ultimaMismoFlujo
                            )
                            && esViaOAuxiliar(
                                    ubicacion
                            )) {
                        retornosTipo++;

                        if (ultimaMismoFlujo.getTipo()
                                != ubicacion.getTipo()) {
                            cambiosTipo++;
                        } else {
                            repeticionesTipo++;
                        }
                    }
                }
            }

            historial.put(
                    keyDia(
                            item.trabajadorId(),
                            item.fecha()
                    ),
                    item.ubicacionId()
            );
        }

        double cobertura =
                porcentaje(
                        intento.asignaciones()
                                .size(),
                        turnos.size()
                );

        double rotacionFlujo =
                transicionesFlujo == 0
                        ? 100.0
                        : porcentaje(
                                cambiosFlujo,
                                transicionesFlujo
                        );

        double rotacionTipo =
                retornosTipo == 0
                        ? 100.0
                        : porcentaje(
                                cambiosTipo,
                                retornosTipo
                        );

        double rotacionCaseta =
                transicionesCaseta == 0
                        ? 100.0
                        : porcentaje(
                                cambiosCaseta,
                                transicionesCaseta
                        );

        double puntuacion =
                (cobertura * 0.40)
                        + (rotacionFlujo * 0.25)
                        + (rotacionTipo * 0.25)
                        + (rotacionCaseta * 0.10)
                        - (
                                duplicidades
                                        * 25.0
                        )
                        - (
                                restriccionesVioladas
                                        * 25.0
                        );

        puntuacion =
                Math.max(
                        0.0,
                        Math.min(
                                100.0,
                                puntuacion
                        )
                );

        return new Calidad(
                redondearUno(
                        puntuacion
                ),
                redondearUno(
                        cobertura
                ),
                redondearUno(
                        rotacionFlujo
                ),
                redondearUno(
                        rotacionTipo
                ),
                redondearUno(
                        rotacionCaseta
                ),
                repeticionesTipo,
                repeticionesExactas,
                duplicidades,
                restriccionesVioladas,
                solucionesEvaluadas,
                cambiosRespectoBase
        );
    }

    private int contarCambiosEntreIntentos(
            IntentoGeneracion base,
            IntentoGeneracion candidato
    ) {
        Map<Long, Long> ubicacionBase =
                base.asignaciones()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        ItemPropuesta::programacionTurnoId,
                                        ItemPropuesta::ubicacionId
                                )
                        );

        int cambios = 0;

        for (ItemPropuesta item :
                candidato.asignaciones()) {
            if (!Objects.equals(
                    ubicacionBase.get(
                            item.programacionTurnoId()
                    ),
                    item.ubicacionId()
            )) {
                cambios++;
            }
        }

        return cambios;
    }

    private boolean esMejorCalidad(
            Calidad candidato,
            Calidad actual
    ) {
        if (actual == null) {
            return true;
        }

        if (candidato.restriccionesVioladas()
                != actual.restriccionesVioladas()) {
            return candidato.restriccionesVioladas()
                    < actual.restriccionesVioladas();
        }

        if (candidato.duplicidades()
                != actual.duplicidades()) {
            return candidato.duplicidades()
                    < actual.duplicidades();
        }

        int cobertura =
                Double.compare(
                        candidato.coberturaPct(),
                        actual.coberturaPct()
                );

        if (cobertura != 0) {
            return cobertura > 0;
        }

        /*
         * Una mejora mínima no justifica reorganizar gran parte del mes.
         * Solo permitimos que la puntuación gane directamente cuando la
         * diferencia es significativa (>= 0.5 puntos).
         *
         * Dentro de esa banda preferimos estabilidad: menor cantidad de
         * asignaciones distintas respecto a la solución base.
         */
        double diferenciaPuntuacion =
                candidato.puntuacion()
                        - actual.puntuacion();

        final double umbralMejoraSignificativa =
                0.5;

        if (Math.abs(
                diferenciaPuntuacion
        ) >= umbralMejoraSignificativa) {
            return diferenciaPuntuacion
                    > 0;
        }

        if (candidato.cambiosRespectoBase()
                != actual.cambiosRespectoBase()) {
            return candidato.cambiosRespectoBase()
                    < actual.cambiosRespectoBase();
        }

        if (candidato.repeticionesTipo()
                != actual.repeticionesTipo()) {
            return candidato.repeticionesTipo()
                    < actual.repeticionesTipo();
        }

        if (candidato.repeticionesExactas()
                != actual.repeticionesExactas()) {
            return candidato.repeticionesExactas()
                    < actual.repeticionesExactas();
        }

        return candidato.puntuacion()
                > actual.puntuacion();
    }

    private double porcentaje(
            int parte,
            int total
    ) {
        if (total <= 0) {
            return 100.0;
        }

        return (
                parte
                        * 100.0
        ) / total;
    }

    private double redondearUno(
            double valor
    ) {
        return Math.round(
                valor * 10.0
        ) / 10.0;
    }

    private IntentoGeneracion repararRotacionMismoFlujo(
            IntentoGeneracion intento,
            List<ProgramacionTurno> turnos,
            List<ProgramacionUbicacion> ubicaciones,
            Map<Long, ConfiguracionCaseta> configCaseta,
            Set<String> restricciones,
            Map<String, Long> historialBase
    ) {
        if (intento.asignaciones().size() < 2) {
            return intento;
        }

        List<ItemPropuesta> reparadas =
                new ArrayList<>(
                        intento.asignaciones()
                );

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

        Map<String, Long> historial =
                new HashMap<>(
                        historialBase
                );

        Map<String, List<Integer>> grupos =
                new TreeMap<>();

        for (int i = 0;
                i < reparadas.size();
                i++) {
            ItemPropuesta item =
                    reparadas.get(i);

            grupos.computeIfAbsent(
                    item.fecha()
                            + "|"
                            + item.turno(),
                    ignored -> new ArrayList<>()
            ).add(i);
        }

        for (List<Integer> indices :
                grupos.values()) {
            if (indices.size() < 2) {
                actualizarHistorialReparacion(
                        reparadas,
                        indices,
                        historial
                );
                continue;
            }

            boolean huboCambio;
            int iteraciones = 0;

            do {
                huboCambio = false;
                int mejorA = -1;
                int mejorB = -1;
                int mejorMejora = 0;

                for (int x = 0;
                        x < indices.size();
                        x++) {
                    int indiceA =
                            indices.get(x);

                    ItemPropuesta itemA =
                            reparadas.get(
                                    indiceA
                            );

                    ProgramacionTurno turnoA =
                            turnoPorId.get(
                                    itemA.programacionTurnoId()
                            );

                    ProgramacionUbicacion ubicacionA =
                            ubicacionPorId.get(
                                    itemA.ubicacionId()
                            );

                    if (turnoA == null
                            || ubicacionA == null
                            || turnoA.getEstado()
                            == EstadoProgramacion.C
                            || !esViaOAuxiliar(
                                    ubicacionA
                            )) {
                        continue;
                    }

                    GrupoFlujoCaseta grupoA =
                            grupo(
                                    configCaseta.get(
                                            ubicacionA.getId()
                                    )
                            );

                    if (!flujoPermiteAlternanciaTipo(
                            grupoA,
                            turnoA.getEstado(),
                            ubicaciones,
                            configCaseta
                    )) {
                        continue;
                    }

                    for (int y = x + 1;
                            y < indices.size();
                            y++) {
                        int indiceB =
                                indices.get(y);

                        ItemPropuesta itemB =
                                reparadas.get(
                                        indiceB
                                );

                        ProgramacionTurno turnoB =
                                turnoPorId.get(
                                        itemB.programacionTurnoId()
                                );

                        ProgramacionUbicacion ubicacionB =
                                ubicacionPorId.get(
                                        itemB.ubicacionId()
                                );

                        if (turnoB == null
                                || ubicacionB == null
                                || !esViaOAuxiliar(
                                        ubicacionB
                                )
                                || ubicacionA.getTipo()
                                == ubicacionB.getTipo()) {
                            continue;
                        }

                        GrupoFlujoCaseta grupoB =
                                grupo(
                                        configCaseta.get(
                                                ubicacionB.getId()
                                        )
                                );

                        /*
                         * Un intercambio nunca cambia el flujo del agente.
                         * Así se conservan intactas las reglas ALTO/BAJO,
                         * incluida la prohibición de tres días seguidos.
                         */
                        if (grupoA
                                != grupoB) {
                            continue;
                        }

                        Long trabajadorA =
                                itemA.trabajadorId();

                        Long trabajadorB =
                                itemB.trabajadorId();

                        if (restricciones.contains(
                                key(
                                        trabajadorA,
                                        ubicacionB.getId()
                                )
                        ) || restricciones.contains(
                                key(
                                        trabajadorB,
                                        ubicacionA.getId()
                                )
                        )) {
                            continue;
                        }

                        int costoAntes =
                                costoRotacionReparacion(
                                        trabajadorA,
                                        itemA.fecha(),
                                        turnoA.getEstado(),
                                        ubicacionA,
                                        grupoA,
                                        historial,
                                        ubicaciones,
                                        configCaseta
                                )
                                        + costoRotacionReparacion(
                                                trabajadorB,
                                                itemB.fecha(),
                                                turnoB.getEstado(),
                                                ubicacionB,
                                                grupoB,
                                                historial,
                                                ubicaciones,
                                                configCaseta
                                        );

                        int costoDespues =
                                costoRotacionReparacion(
                                        trabajadorA,
                                        itemA.fecha(),
                                        turnoA.getEstado(),
                                        ubicacionB,
                                        grupoA,
                                        historial,
                                        ubicaciones,
                                        configCaseta
                                )
                                        + costoRotacionReparacion(
                                                trabajadorB,
                                                itemB.fecha(),
                                                turnoB.getEstado(),
                                                ubicacionA,
                                                grupoB,
                                                historial,
                                                ubicaciones,
                                                configCaseta
                                        );

                        int mejora =
                                costoAntes
                                        - costoDespues;

                        if (mejora
                                > mejorMejora) {
                            mejorMejora =
                                    mejora;
                            mejorA =
                                    indiceA;
                            mejorB =
                                    indiceB;
                        }
                    }
                }

                if (mejorA >= 0
                        && mejorB >= 0
                        && mejorMejora > 0) {
                    ItemPropuesta itemA =
                            reparadas.get(
                                    mejorA
                            );

                    ItemPropuesta itemB =
                            reparadas.get(
                                    mejorB
                            );

                    reparadas.set(
                            mejorA,
                            intercambiarUbicacion(
                                    itemA,
                                    itemB
                            )
                    );

                    reparadas.set(
                            mejorB,
                            intercambiarUbicacion(
                                    itemB,
                                    itemA
                            )
                    );

                    huboCambio =
                            true;
                }

                iteraciones++;
            } while (huboCambio
                    && iteraciones < 20);

            actualizarHistorialReparacion(
                    reparadas,
                    indices,
                    historial
            );
        }

        return new IntentoGeneracion(
                List.copyOf(
                        reparadas
                ),
                intento.conflictos()
        );
    }

    private IntentoGeneracion repararIntercambiosGlobales(
            IntentoGeneracion intento,
            List<ProgramacionTurno> turnos,
            List<ProgramacionUbicacion> ubicaciones,
            Map<Long, ConfiguracionCaseta> configCaseta,
            Set<String> restricciones,
            Configuracion config,
            Map<String, Long> historialBase
    ) {
        if (intento.asignaciones().size() < 2) {
            return intento;
        }

        List<ItemPropuesta> reparadas =
                new ArrayList<>(
                        intento.asignaciones()
                );

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

        Map<Long, List<ProgramacionTurno>> turnosPorTrabajador =
                turnos.stream()
                        .collect(
                                Collectors.groupingBy(
                                        item ->
                                                item.getTrabajador()
                                                        .getId()
                                )
                        );

        turnosPorTrabajador.values()
                .forEach(lista ->
                        lista.sort(
                                Comparator.comparing(
                                        ProgramacionTurno::getFecha
                                )
                        )
                );

        Map<String, Long> mapa =
                new HashMap<>(
                        historialBase
                );

        for (ItemPropuesta item :
                reparadas) {
            mapa.put(
                    keyDia(
                            item.trabajadorId(),
                            item.fecha()
                    ),
                    item.ubicacionId()
            );
        }

        Map<String, List<Integer>> grupos =
                new TreeMap<>();

        for (int i = 0;
                i < reparadas.size();
                i++) {
            ItemPropuesta item =
                    reparadas.get(i);

            grupos.computeIfAbsent(
                    item.fecha()
                            + "|"
                            + item.turno(),
                    ignored ->
                            new ArrayList<>()
            ).add(i);
        }

        /*
         * Varias rondas cortas son suficientes: cada swap aceptado debe
         * reducir estrictamente el costo, por lo que no puede oscilar.
         */
        for (int ronda = 0;
                ronda < 4;
                ronda++) {
            boolean cambioEnRonda =
                    false;

            for (List<Integer> indices :
                    grupos.values()) {
                if (indices.size() < 2) {
                    continue;
                }

                int mejorA = -1;
                int mejorB = -1;
                int mejorMejora = 0;

                for (int x = 0;
                        x < indices.size();
                        x++) {
                    int indiceA =
                            indices.get(x);

                    ItemPropuesta itemA =
                            reparadas.get(
                                    indiceA
                            );

                    ProgramacionTurno turnoA =
                            turnoPorId.get(
                                    itemA.programacionTurnoId()
                            );

                    ProgramacionUbicacion ubicacionA =
                            ubicacionPorId.get(
                                    itemA.ubicacionId()
                            );

                    if (turnoA == null
                            || ubicacionA == null
                            || !esViaOAuxiliar(
                                    ubicacionA
                            )) {
                        continue;
                    }

                    for (int y = x + 1;
                            y < indices.size();
                            y++) {
                        int indiceB =
                                indices.get(y);

                        ItemPropuesta itemB =
                                reparadas.get(
                                        indiceB
                                );

                        ProgramacionTurno turnoB =
                                turnoPorId.get(
                                        itemB.programacionTurnoId()
                                );

                        ProgramacionUbicacion ubicacionB =
                                ubicacionPorId.get(
                                        itemB.ubicacionId()
                                );

                        if (turnoB == null
                                || ubicacionB == null
                                || !esViaOAuxiliar(
                                        ubicacionB
                                )
                                || Objects.equals(
                                        ubicacionA.getId(),
                                        ubicacionB.getId()
                                )) {
                            continue;
                        }

                        GrupoFlujoCaseta grupoA =
                                grupo(
                                        configCaseta.get(
                                                ubicacionA.getId()
                                        )
                                );

                        GrupoFlujoCaseta grupoB =
                                grupo(
                                        configCaseta.get(
                                                ubicacionB.getId()
                                        )
                                );

                        /*
                         * Turno C solo rota entre vías del mismo flujo.
                         * En A/B sí permitimos un cruce ALTO <-> BAJO.
                         */
                        if (turnoA.getEstado()
                                == EstadoProgramacion.C
                                && grupoA != grupoB) {
                            continue;
                        }

                        Long trabajadorA =
                                itemA.trabajadorId();

                        Long trabajadorB =
                                itemB.trabajadorId();

                        if (restricciones.contains(
                                key(
                                        trabajadorA,
                                        ubicacionB.getId()
                                )
                        ) || restricciones.contains(
                                key(
                                        trabajadorB,
                                        ubicacionA.getId()
                                )
                        )) {
                            continue;
                        }

                        String keyA =
                                keyDia(
                                        trabajadorA,
                                        itemA.fecha()
                                );

                        String keyB =
                                keyDia(
                                        trabajadorB,
                                        itemB.fecha()
                                );

                        int costoAntes =
                                costoTrabajadorReparacion(
                                        trabajadorA,
                                        turnosPorTrabajador
                                                .getOrDefault(
                                                        trabajadorA,
                                                        List.of()
                                                ),
                                        mapa,
                                        ubicaciones,
                                        configCaseta
                                )
                                        + costoTrabajadorReparacion(
                                                trabajadorB,
                                                turnosPorTrabajador
                                                        .getOrDefault(
                                                                trabajadorB,
                                                                List.of()
                                                        ),
                                                mapa,
                                                ubicaciones,
                                                configCaseta
                                        );

                        Long anteriorA =
                                mapa.put(
                                        keyA,
                                        ubicacionB.getId()
                                );

                        Long anteriorB =
                                mapa.put(
                                        keyB,
                                        ubicacionA.getId()
                                );

                        boolean valido =
                                intercambioGlobalValido(
                                        trabajadorA,
                                        itemA.fecha(),
                                        turnoA.getEstado(),
                                        ubicacionB,
                                        mapa,
                                        ubicaciones,
                                        configCaseta,
                                        config
                                )
                                        && intercambioGlobalValido(
                                                trabajadorB,
                                                itemB.fecha(),
                                                turnoB.getEstado(),
                                                ubicacionA,
                                                mapa,
                                                ubicaciones,
                                                configCaseta,
                                                config
                                        );

                        int costoDespues =
                                valido
                                        ? costoTrabajadorReparacion(
                                                trabajadorA,
                                                turnosPorTrabajador
                                                        .getOrDefault(
                                                                trabajadorA,
                                                                List.of()
                                                        ),
                                                mapa,
                                                ubicaciones,
                                                configCaseta
                                        )
                                                + costoTrabajadorReparacion(
                                                        trabajadorB,
                                                        turnosPorTrabajador
                                                                .getOrDefault(
                                                                        trabajadorB,
                                                                        List.of()
                                                                ),
                                                        mapa,
                                                        ubicaciones,
                                                        configCaseta
                                                )
                                        : Integer.MAX_VALUE;

                        if (anteriorA == null) {
                            mapa.remove(
                                    keyA
                            );
                        } else {
                            mapa.put(
                                    keyA,
                                    anteriorA
                            );
                        }

                        if (anteriorB == null) {
                            mapa.remove(
                                    keyB
                            );
                        } else {
                            mapa.put(
                                    keyB,
                                    anteriorB
                            );
                        }

                        if (!valido) {
                            continue;
                        }

                        int mejora =
                                costoAntes
                                        - costoDespues;

                        if (mejora
                                > mejorMejora) {
                            mejorMejora =
                                    mejora;
                            mejorA =
                                    indiceA;
                            mejorB =
                                    indiceB;
                        }
                    }
                }

                if (mejorA >= 0
                        && mejorB >= 0
                        && mejorMejora > 0) {
                    ItemPropuesta itemA =
                            reparadas.get(
                                    mejorA
                            );

                    ItemPropuesta itemB =
                            reparadas.get(
                                    mejorB
                            );

                    reparadas.set(
                            mejorA,
                            intercambiarUbicacion(
                                    itemA,
                                    itemB
                            )
                    );

                    reparadas.set(
                            mejorB,
                            intercambiarUbicacion(
                                    itemB,
                                    itemA
                            )
                    );

                    mapa.put(
                            keyDia(
                                    itemA.trabajadorId(),
                                    itemA.fecha()
                            ),
                            itemB.ubicacionId()
                    );

                    mapa.put(
                            keyDia(
                                    itemB.trabajadorId(),
                                    itemB.fecha()
                            ),
                            itemA.ubicacionId()
                    );

                    cambioEnRonda =
                            true;
                }
            }

            if (!cambioEnRonda) {
                break;
            }
        }

        return new IntentoGeneracion(
                List.copyOf(
                        reparadas
                ),
                intento.conflictos()
        );
    }

    private boolean intercambioGlobalValido(
            Long trabajadorId,
            LocalDate fecha,
            EstadoProgramacion turno,
            ProgramacionUbicacion candidata,
            Map<String, Long> mapa,
            List<ProgramacionUbicacion> ubicaciones,
            Map<Long, ConfiguracionCaseta> configCaseta,
            Configuracion config
    ) {
        if (candidata == null) {
            return false;
        }

        /*
         * El conjunto de casetas del turno no cambia durante un swap,
         * por eso cobertura, duplicidad y jerarquía permanecen intactas.
         * Aquí validamos únicamente las reglas que dependen del agente.
         */
        if (turno != EstadoProgramacion.C) {
            GrupoFlujoCaseta grupo =
                    grupoDeUbicacion(
                            candidata.getId(),
                            configCaseta
                    );

            if (grupo
                    != GrupoFlujoCaseta.SIN_CLASIFICAR
                    && !cumpleMaxConsecutivosFlujo(
                            trabajadorId,
                            fecha,
                            grupo,
                            mapa,
                            configCaseta,
                            config.maxConsecutivos()
                    )) {
                return false;
            }

            ConfiguracionCaseta especifica =
                    configCaseta.get(
                            candidata.getId()
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

            if (conteoUbicacionSemana(
                    trabajadorId,
                    candidata.getId(),
                    fecha,
                    mapa
            ) > maxSemana) {
                return false;
            }

            if (conteoUbicacionMes(
                    trabajadorId,
                    candidata.getId(),
                    fecha,
                    mapa
            ) > maxMes) {
                return false;
            }
        }

        return true;
    }

    private boolean cumpleMaxConsecutivosFlujo(
            Long trabajadorId,
            LocalDate fecha,
            GrupoFlujoCaseta grupo,
            Map<String, Long> mapa,
            Map<Long, ConfiguracionCaseta> configCaseta,
            int maxConsecutivos
    ) {
        int consecutivos =
                1;

        LocalDate cursor =
                fecha.minusDays(1);

        while (grupoDeUbicacion(
                mapa.get(
                        keyDia(
                                trabajadorId,
                                cursor
                        )
                ),
                configCaseta
        ) == grupo) {
            consecutivos++;
            cursor =
                    cursor.minusDays(1);
        }

        cursor =
                fecha.plusDays(1);

        while (grupoDeUbicacion(
                mapa.get(
                        keyDia(
                                trabajadorId,
                                cursor
                        )
                ),
                configCaseta
        ) == grupo) {
            consecutivos++;
            cursor =
                    cursor.plusDays(1);
        }

        return consecutivos
                <= maxConsecutivos;
    }

    private int conteoUbicacionSemana(
            Long trabajadorId,
            Long ubicacionId,
            LocalDate fecha,
            Map<String, Long> mapa
    ) {
        int semana =
                semanaDelMes(
                        fecha
                );

        YearMonth ym =
                YearMonth.from(
                        fecha
                );

        int total = 0;

        for (int dia = 1;
                dia <= ym.lengthOfMonth();
                dia++) {
            LocalDate cursor =
                    ym.atDay(
                            dia
                    );

            if (semanaDelMes(
                    cursor
            ) != semana) {
                continue;
            }

            if (Objects.equals(
                    mapa.get(
                            keyDia(
                                    trabajadorId,
                                    cursor
                            )
                    ),
                    ubicacionId
            )) {
                total++;
            }
        }

        return total;
    }

    private int conteoUbicacionMes(
            Long trabajadorId,
            Long ubicacionId,
            LocalDate fecha,
            Map<String, Long> mapa
    ) {
        YearMonth ym =
                YearMonth.from(
                        fecha
                );

        int total = 0;

        for (int dia = 1;
                dia <= ym.lengthOfMonth();
                dia++) {
            LocalDate cursor =
                    ym.atDay(
                            dia
                    );

            if (Objects.equals(
                    mapa.get(
                            keyDia(
                                    trabajadorId,
                                    cursor
                            )
                    ),
                    ubicacionId
            )) {
                total++;
            }
        }

        return total;
    }

    private int costoTrabajadorReparacion(
            Long trabajadorId,
            List<ProgramacionTurno> turnosTrabajador,
            Map<String, Long> mapa,
            List<ProgramacionUbicacion> ubicaciones,
            Map<Long, ConfiguracionCaseta> configCaseta
    ) {
        int costo =
                0;

        Map<String, Integer> usosPorCaseta =
                new HashMap<>();

        Map<GrupoFlujoCaseta, int[]> tiposPorFlujo =
                new EnumMap<>(
                        GrupoFlujoCaseta.class
                );

        for (ProgramacionTurno turno :
                turnosTrabajador) {
            LocalDate fecha =
                    turno.getFecha();

            Long ubicacionId =
                    mapa.get(
                            keyDia(
                                    trabajadorId,
                                    fecha
                            )
                    );

            ProgramacionUbicacion actual =
                    ubicacionPorId(
                            ubicaciones,
                            ubicacionId
                    );

            if (actual == null) {
                continue;
            }

            GrupoFlujoCaseta grupoActual =
                    grupoDeUbicacion(
                            actual.getId(),
                            configCaseta
                    );

            usosPorCaseta.merge(
                    String.valueOf(
                            actual.getId()
                    ),
                    1,
                    Integer::sum
            );

            Long ayerId =
                    mapa.get(
                            keyDia(
                                    trabajadorId,
                                    fecha.minusDays(1)
                            )
                    );

            if (Objects.equals(
                    ayerId,
                    actual.getId()
            )) {
                costo +=
                        700;
            }

            if (turno.getEstado()
                    != EstadoProgramacion.C) {
                GrupoFlujoCaseta grupoAyer =
                        grupoDeUbicacion(
                                ayerId,
                                configCaseta
                        );

                if (grupoActual
                        != GrupoFlujoCaseta.SIN_CLASIFICAR
                        && grupoAyer
                        != GrupoFlujoCaseta.SIN_CLASIFICAR) {
                    if (grupoActual
                            == grupoAyer) {
                        costo +=
                                180;
                    } else {
                        costo -=
                                40;
                    }
                }

                if (flujoPermiteAlternanciaTipo(
                        grupoActual,
                        turno.getEstado(),
                        ubicaciones,
                        configCaseta
                )) {
                    ProgramacionUbicacion ultimaMismoFlujo =
                            ultimaUbicacionEnMismoFlujo(
                                    trabajadorId,
                                    fecha,
                                    grupoActual,
                                    mapa,
                                    ubicaciones,
                                    configCaseta
                            );

                    if (ultimaMismoFlujo != null
                            && esViaOAuxiliar(
                                    ultimaMismoFlujo
                            )) {
                        if (ultimaMismoFlujo.getTipo()
                                == actual.getTipo()) {
                            costo +=
                                    1_000;
                        } else {
                            costo -=
                                    120;
                        }
                    }

                    int[] tipos =
                            tiposPorFlujo
                                    .computeIfAbsent(
                                            grupoActual,
                                            ignored ->
                                                    new int[]{
                                                            0,
                                                            0
                                                    }
                                    );

                    if (actual.getTipo()
                            == TipoUbicacion.VIA) {
                        tipos[0]++;
                    } else if (actual.getTipo()
                            == TipoUbicacion.AUXILIAR) {
                        tipos[1]++;
                    }
                }
            }
        }

        for (Integer usos :
                usosPorCaseta.values()) {
            /*
             * Penalización convexa: repartir 6 usos como 2/2/2 resulta
             * mejor que concentrarlos como 4/1/1.
             */
            costo +=
                    usos
                            * usos
                            * 18;
        }

        for (int[] tipos :
                tiposPorFlujo.values()) {
            costo +=
                    Math.abs(
                            tipos[0]
                                    - tipos[1]
                    ) * 90;
        }

        return costo;
    }

    private ItemPropuesta intercambiarUbicacion(
            ItemPropuesta agente,
            ItemPropuesta origenUbicacion
    ) {
        return new ItemPropuesta(
                agente.programacionTurnoId(),
                agente.trabajadorId(),
                agente.codigoTrabajador(),
                agente.trabajador(),
                agente.fecha(),
                agente.turno(),
                origenUbicacion.ubicacionId(),
                origenUbicacion.ubicacionCodigo(),
                origenUbicacion.ubicacionNombre(),
                origenUbicacion.grupoFlujo(),
                agente.puntaje()
        );
    }

    private int costoRotacionReparacion(
            Long trabajadorId,
            LocalDate fecha,
            EstadoProgramacion turno,
            ProgramacionUbicacion ubicacion,
            GrupoFlujoCaseta grupo,
            Map<String, Long> historial,
            List<ProgramacionUbicacion> ubicaciones,
            Map<Long, ConfiguracionCaseta> configCaseta
    ) {
        int costo = 0;

        if (flujoPermiteAlternanciaTipo(
                grupo,
                turno,
                ubicaciones,
                configCaseta
        )) {
            ProgramacionUbicacion ultimaMismoFlujo =
                    ultimaUbicacionEnMismoFlujo(
                            trabajadorId,
                            fecha,
                            grupo,
                            historial,
                            ubicaciones,
                            configCaseta
                    );

            if (ultimaMismoFlujo != null
                    && esViaOAuxiliar(
                            ultimaMismoFlujo
                    )
                    && ultimaMismoFlujo.getTipo()
                    == ubicacion.getTipo()) {
                /*
                 * Este es el defecto principal que queremos reparar:
                 * BAJO/VIA -> ALTO/... -> BAJO/VIA, existiendo AUX.
                 */
                costo +=
                        1_000;
            }

            costo +=
                    desequilibrioTipoProyectado(
                            trabajadorId,
                            fecha,
                            grupo,
                            ubicacion.getTipo(),
                            historial,
                            ubicaciones,
                            configCaseta
                    ) * 55;
        }

        Long ayer =
                historial.get(
                        keyDia(
                                trabajadorId,
                                fecha.minusDays(1)
                        )
                );

        if (Objects.equals(
                ayer,
                ubicacion.getId()
        )) {
            costo +=
                    260;
        }

        int usosSemana =
                usosUbicacionEnVentana(
                        trabajadorId,
                        ubicacion.getId(),
                        fecha,
                        7,
                        historial
                );

        int usosMes =
                usosUbicacionEnVentana(
                        trabajadorId,
                        ubicacion.getId(),
                        fecha,
                        31,
                        historial
                );

        costo +=
                (usosSemana * 65)
                        + (usosMes * 15);

        return costo;
    }

    private int penalizacionBalanceTipoMismoFlujo(
            Long trabajadorId,
            LocalDate fecha,
            GrupoFlujoCaseta grupo,
            ProgramacionUbicacion candidata,
            Map<String, Long> historial,
            List<ProgramacionUbicacion> ubicaciones,
            Map<Long, ConfiguracionCaseta> configCaseta,
            boolean alternanciaDisponible
    ) {
        if (!alternanciaDisponible
                || candidata == null
                || !esViaOAuxiliar(
                        candidata
                )) {
            return 0;
        }

        return desequilibrioTipoProyectado(
                trabajadorId,
                fecha,
                grupo,
                candidata.getTipo(),
                historial,
                ubicaciones,
                configCaseta
        ) * 55;
    }

    private int desequilibrioTipoProyectado(
            Long trabajadorId,
            LocalDate fecha,
            GrupoFlujoCaseta grupo,
            TipoUbicacion tipoCandidato,
            Map<String, Long> historial,
            List<ProgramacionUbicacion> ubicaciones,
            Map<Long, ConfiguracionCaseta> configCaseta
    ) {
        int vias = 0;
        int auxiliares = 0;

        LocalDate cursor =
                fecha.minusDays(1);

        LocalDate limite =
                fecha.minusDays(31);

        while (!cursor.isBefore(
                limite
        )) {
            Long ubicacionId =
                    historial.get(
                            keyDia(
                                    trabajadorId,
                                    cursor
                            )
                    );

            if (ubicacionId != null
                    && grupoDeUbicacion(
                            ubicacionId,
                            configCaseta
                    ) == grupo) {
                ProgramacionUbicacion previa =
                        ubicacionPorId(
                                ubicaciones,
                                ubicacionId
                        );

                if (previa != null) {
                    if (previa.getTipo()
                            == TipoUbicacion.VIA) {
                        vias++;
                    } else if (previa.getTipo()
                            == TipoUbicacion.AUXILIAR) {
                        auxiliares++;
                    }
                }
            }

            cursor =
                    cursor.minusDays(1);
        }

        if (tipoCandidato
                == TipoUbicacion.VIA) {
            vias++;
        } else if (tipoCandidato
                == TipoUbicacion.AUXILIAR) {
            auxiliares++;
        }

        return Math.abs(
                vias - auxiliares
        );
    }

    private int usosUbicacionEnVentana(
            Long trabajadorId,
            Long ubicacionId,
            LocalDate fecha,
            int dias,
            Map<String, Long> historial
    ) {
        int usos = 0;

        for (int offset = 1;
                offset <= dias;
                offset++) {
            if (Objects.equals(
                    historial.get(
                            keyDia(
                                    trabajadorId,
                                    fecha.minusDays(
                                            offset
                                    )
                            )
                    ),
                    ubicacionId
            )) {
                usos++;
            }
        }

        return usos;
    }

    private void actualizarHistorialReparacion(
            List<ItemPropuesta> asignaciones,
            List<Integer> indices,
            Map<String, Long> historial
    ) {
        for (Integer indice :
                indices) {
            ItemPropuesta item =
                    asignaciones.get(
                            indice
                    );

            historial.put(
                    keyDia(
                            item.trabajadorId(),
                            item.fecha()
                    ),
                    item.ubicacionId()
            );
        }
    }

    private boolean flujoPermiteAlternanciaTipo(
            GrupoFlujoCaseta grupo,
            EstadoProgramacion turno,
            List<ProgramacionUbicacion> ubicaciones,
            Map<Long, ConfiguracionCaseta> configCaseta
    ) {
        if (grupo == null
                || grupo
                == GrupoFlujoCaseta.SIN_CLASIFICAR
                || turno
                == EstadoProgramacion.C) {
            return false;
        }

        boolean tieneVia =
                false;
        boolean tieneAuxiliar =
                false;

        for (ProgramacionUbicacion ubicacion :
                ubicaciones) {
            if (!Boolean.TRUE.equals(
                    ubicacion.getActivo()
            )) {
                continue;
            }

            if (grupo(
                    configCaseta.get(
                            ubicacion.getId()
                    )
            ) != grupo) {
                continue;
            }

            if (ubicacion.getTipo()
                    == TipoUbicacion.VIA) {
                tieneVia =
                        true;
            } else if (ubicacion.getTipo()
                    == TipoUbicacion.AUXILIAR
                    && permiteTurno(
                            ubicacion,
                            turno
                    )) {
                tieneAuxiliar =
                        true;
            }

            if (tieneVia
                    && tieneAuxiliar) {
                return true;
            }
        }

        return false;
    }

    private void validarRestriccionesFinales(
            List<ItemPropuesta> asignaciones,
            Set<String> restricciones
    ) {
        for (ItemPropuesta item :
                asignaciones) {
            if (restricciones.contains(
                    key(
                            item.trabajadorId(),
                            item.ubicacionId()
                    )
            )) {
                throw new ProgramacionValidationException(
                        "La reparación intentó asignar "
                                + item.ubicacionCodigo()
                                + " a "
                                + item.trabajador()
                                + ", pero existe una restricción activa"
                );
            }
        }
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
            boolean flujoEstricto,
            boolean coberturaForzada
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
                        coberturaForzada
                                ? 12_000
                                : (
                                        flujoEstricto
                                                ? 1_200
                                                : 6_000
                                ),
                        coberturaForzada
                                ? 4_500L
                                : (
                                        flujoEstricto
                                                ? 1_000L
                                                : 2_800L
                                )
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
                coberturaForzada,
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
            boolean coberturaForzada,
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
                        coberturaForzada,
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
                    coberturaForzada,
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
            boolean coberturaForzada,
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
                            coberturaForzada,
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
            boolean coberturaForzada,
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
                        coberturaForzada,
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
                        coberturaForzada
                                ? 650L
                                : 300L,
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
                coberturaForzada,
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
            boolean coberturaForzada,
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
                                coberturaForzada,
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
            boolean coberturaForzada,
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
                            coberturaForzada,
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
                    coberturaForzada,
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
                coberturaForzada,
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
            boolean coberturaForzada,
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

            if (!coberturaForzada
                    && Objects.equals(
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

            ProgramacionUbicacion ultimaUbicacionMismoFlujo =
                    ultimaUbicacionEnMismoFlujo(
                            trabajadorId,
                            turno.getFecha(),
                            grupo,
                            asignacionPorDiaAgente,
                            ubicaciones,
                            configCaseta
                    );

            /*
             * Rotación por tipo DENTRO DEL MISMO FLUJO.
             *
             * Ejemplo ideal:
             *   Día 1: BAJO / VIA
             *   Día 2: ALTO / VIA o AUXILIAR
             *   Día 3: BAJO / AUXILIAR
             *
             * Es decir, al volver a BAJO se compara contra la última
             * asignación BAJO, no contra el día anterior. Lo mismo para ALTO.
             * VIA y AUXILIAR deben alternarse siempre que exista una
             * combinación válida. En flexible/reparación puede repetirse
             * el tipo para no dejar personal sin asignación.
             */
            boolean alternanciaTipoDisponible =
                    !turnoC
                            && flujoPermiteAlternanciaTipo(
                                    grupo,
                                    turno.getEstado(),
                                    ubicaciones,
                                    configCaseta
                            );

            boolean repiteTipoEnMismoFlujo =
                    alternanciaTipoDisponible
                            && esViaOAuxiliar(
                                    ubicacion
                            )
                            && ultimaUbicacionMismoFlujo != null
                            && esViaOAuxiliar(
                                    ultimaUbicacionMismoFlujo
                            )
                            && ultimaUbicacionMismoFlujo.getTipo()
                            == ubicacion.getTipo();

            if (flujoEstricto
                    && repiteTipoEnMismoFlujo) {
                continue;
            }

            /*
             * Alternancia de flujo para A/B:
             *
             * 1) En la pasada estricta primero intentamos SIEMPRE cambiar
             *    de flujo respecto al día anterior.
             * 2) Si esa combinación no es posible, la pasada flexible
             *    puede repetir el mismo flujo una sola vez.
             * 3) Al volver a un flujo, VIA/AUXILIAR se alterna respecto
             *    a la última asignación que tuvo ese mismo flujo.
             * 4) Nunca permitimos tres días consecutivos con el mismo flujo.
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
                 * - La pasada flexible admite ALTO -> ALTO o BAJO -> BAJO
                 *   cambiando preferentemente el tipo de ubicación.
                 * - La reparación puede repetir también el mismo tipo si
                 *   es la única forma de asignar al agente.
                 * - Nunca se permiten tres días seguidos del mismo flujo.
                 */
                if (seriaTercerDiaMismoFlujo) {
                    continue;
                }

                if (flujoEstricto
                        || (
                                !coberturaForzada
                                        && repiteTipoUbicacion
                        )) {
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
                                + (
                                        alternanciaTipoDisponible
                                                ? penalizacionTipoEnMismoFlujo(
                                                        ultimaUbicacionMismoFlujo,
                                                        ubicacion,
                                                        coberturaForzada
                                                )
                                                : 0
                                )
                                + penalizacionBalanceTipoMismoFlujo(
                                        trabajadorId,
                                        turno.getFecha(),
                                        grupo,
                                        ubicacion,
                                        asignacionPorDiaAgente,
                                        ubicaciones,
                                        configCaseta,
                                        alternanciaTipoDisponible
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

    private int penalizacionTipoEnMismoFlujo(
            ProgramacionUbicacion ultimaMismoFlujo,
            ProgramacionUbicacion actual,
            boolean coberturaForzada
    ) {
        if (ultimaMismoFlujo == null
                || actual == null
                || !esViaOAuxiliar(
                        ultimaMismoFlujo
                )
                || !esViaOAuxiliar(
                        actual
                )) {
            return 0;
        }

        if (ultimaMismoFlujo.getTipo()
                != actual.getTipo()) {
            return -120;
        }

        /*
         * Repetir VIA/VIA o AUX/AUX al volver al mismo flujo es una
         * excepción. No se bloquea en las fases de recuperación porque
         * la prioridad superior sigue siendo asignar a todo el personal.
         */
        return coberturaForzada
                ? 650
                : 900;
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

    private ProgramacionUbicacion ultimaUbicacionEnMismoFlujo(
            Long trabajadorId,
            LocalDate fecha,
            GrupoFlujoCaseta flujoBuscado,
            Map<String, Long> asignacionPorDiaAgente,
            List<ProgramacionUbicacion> ubicaciones,
            Map<Long, ConfiguracionCaseta> configCaseta
    ) {
        if (flujoBuscado == null
                || flujoBuscado
                == GrupoFlujoCaseta.SIN_CLASIFICAR) {
            return null;
        }

        LocalDate cursor =
                fecha.minusDays(1);

        LocalDate limite =
                fecha.minusDays(31);

        while (!cursor.isBefore(
                limite
        )) {
            Long ubicacionId =
                    asignacionPorDiaAgente.get(
                            keyDia(
                                    trabajadorId,
                                    cursor
                            )
                    );

            if (ubicacionId != null
                    && grupoDeUbicacion(
                            ubicacionId,
                            configCaseta
                    ) == flujoBuscado) {
                return ubicacionPorId(
                        ubicaciones,
                        ubicacionId
                );
            }

            cursor =
                    cursor.minusDays(1);
        }

        return null;
    }

    private boolean esViaOAuxiliar(
            ProgramacionUbicacion ubicacion
    ) {
        return ubicacion != null
                && (
                        ubicacion.getTipo()
                                == TipoUbicacion.VIA
                                || ubicacion.getTipo()
                                == TipoUbicacion.AUXILIAR
                );
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
