package com.sigo.programacion.service;

import com.sigo.personal.entity.Plaza;
import com.sigo.personal.entity.RolSistema;
import com.sigo.personal.entity.Trabajador;
import com.sigo.personal.repository.PlazaRepository;
import com.sigo.personal.repository.TrabajadorRepository;
import com.sigo.programacion.dto.generador.ProgramacionGeneradorDto.*;
import com.sigo.programacion.entity.AgenteProgramacionExcepcion;
import com.sigo.programacion.entity.EstadoProgramacion;
import com.sigo.programacion.entity.GrupoProgramacion;
import com.sigo.programacion.entity.ProgramacionSecuenciaAgente;
import com.sigo.programacion.entity.ProgramacionTurno;
import com.sigo.programacion.repository.AgenteProgramacionExcepcionRepository;
import com.sigo.programacion.repository.ProgramacionSecuenciaAgenteRepository;
import com.sigo.programacion.repository.ProgramacionTurnoRepository;
import com.sigo.security.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgramacionGeneradorService {

    /*
     * ============================================================
     * REGLAS DEL GENERADOR
     * ============================================================
     *
     * FULL TIME
     * ----------
     * - Ciclo estructural de 8 días:
     *
     *      1 2 3 4 5 6 7 8
     *      T T T T T T D D
     *
     * - Posiciones laborales 1-4:
     *      A / B
     *
     * - Posiciones laborales 5-6:
     *      A / B / C
     *
     * - Por lo tanto C solamente puede aparecer en los dos
     *   últimos días laborales del bloque.
     *
     * - Máximo 2 C por bloque.
     *
     * - Patrón objetivo/preferido:
     *      A A B B C C D D
     *
     * - La progresión normal es A -> B -> C -> D.
     * - Si el agente permite A, un bloque nuevo no debe comenzar en B.
     * - Una vez que el bloque pasa de A a B, no vuelve a A.
     * - Una vez que entra a C, no vuelve a A ni B antes del descanso.
     * - Se favorecen pares AA / BB / CC cuando la cobertura lo permite.
     * - Por cada secuencia y semana ISO se reserva como mínimo un FT
     *   que no realizará turno C. La persona protegida rota por semana.
     *
     *
     * REGLA DE TRANSICIÓN PARA TODOS
     * --------------------------------
     *
     *      C -> A       NO
     *      C -> B       NO
     *      C -> C       SI
     *      C -> D -> A  SI
     *      C -> D -> B  SI
     *
     * Solo importa el día calendario inmediatamente anterior.
     *
     *
     * PART TIME
     * ---------
     * - No sigue ciclo 6x2.
     * - Exactamente 3 días trabajados por semana.
     * - Puede hacer C en sus 3 días.
     * - También respeta C -> A/B prohibido de forma directa.
     *
     *
     * NOVEDADES
     * ---------
     * V / COM / DM / LIC son overlays.
     * No mueven ni reinician el ciclo 6x2.
     */

    private static final int TAMANO_CICLO = 8;
    private static final int DIAS_TRABAJO = 6;

    /*
     * Índices internos del ciclo:
     *
     * 0 = laboral 1
     * 1 = laboral 2
     * 2 = laboral 3
     * 3 = laboral 4
     * 4 = laboral 5 -> puede C
     * 5 = laboral 6 -> puede C
     * 6 = D
     * 7 = D
     */
    private static final int PRIMERA_POSICION_C = 4;

    private static final int MAX_C_FULL_TIME_BLOQUE = 2;
    private static final int DIAS_PART_TIME_SEMANA = 3;

    /*
     * Por cada secuencia FT y semana ISO se protege como mínimo
     * a un trabajador para que no realice turno C. La selección
     * rota semanalmente para no castigar siempre al mismo agente.
     */
    private static final int MIN_SIN_C_POR_SECUENCIA_SEMANA = 1;

    private final ProgramacionTurnoRepository programacionRepo;
    private final ProgramacionSecuenciaAgenteRepository secuenciaRepo;
    private final AgenteProgramacionExcepcionRepository excepcionRepo;
    private final TrabajadorRepository trabajadorRepo;
    private final PlazaRepository plazaRepo;
    private final CurrentUserService currentUser;

    // ============================================================
    // GENERACIÓN PRINCIPAL
    // ============================================================

    @Transactional(readOnly = true)
    public ProgramacionPropuestaResponse generar(
            GenerarProgramacionRequest request
    ) {

        requireSupervisor();
        validarRequest(request);

        Plaza plaza = plazaRepo
                .findById(request.plazaId())
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .orElseThrow(() -> bad("Plaza no válida o inactiva"));

        YearMonth yearMonth = YearMonth.of(
                request.anio(),
                request.mes()
        );

        LocalDate inicio = yearMonth.atDay(1);
        LocalDate fin = yearMonth.atEndOfMonth();

        /*
         * Necesitamos historial para:
         *
         * 1. Reconstruir continuidad 6x2.
         * 2. Revisar el día anterior al inicio del mes.
         * 3. Controlar PT en semanas que cruzan meses.
         */
        LocalDate inicioHistorico = inicio.minusDays(64);

        // ========================================================
        // AGENTES
        // ========================================================

        List<Trabajador> agentes =
                trabajadorRepo.findAgentesByPlaza(plaza.getId());

        Map<Long, Trabajador> agentesPorId =
                agentes.stream()
                        .collect(
                                Collectors.toMap(
                                        Trabajador::getId,
                                        Function.identity()
                                )
                        );

        // ========================================================
        // SECUENCIAS
        // ========================================================

        List<ProgramacionSecuenciaAgente> secuencias =
                secuenciaRepo.findActivasByPlazaId(plaza.getId());

        Map<Long, ProgramacionSecuenciaAgente> secuenciaPorAgente =
                secuencias.stream()
                        .collect(
                                Collectors.toMap(
                                        s -> s.getAgente().getId(),
                                        Function.identity()
                                )
                        );

        // ========================================================
        // EXCEPCIONES
        // ========================================================

        List<AgenteProgramacionExcepcion> excepciones =
                excepcionRepo
                        .findByPlazaIdAndActivoTrueOrderByTrabajadorNombreCompletoAsc(
                                plaza.getId()
                        );

        Map<Long, AgenteProgramacionExcepcion> excepcionPorAgente =
                excepciones.stream()
                        .collect(
                                Collectors.toMap(
                                        e -> e.getTrabajador().getId(),
                                        Function.identity()
                                )
                        );

        // ========================================================
        // HISTÓRICO
        // ========================================================

        List<ProgramacionTurno> historico =
                programacionRepo.findMes(
                        plaza.getId(),
                        inicioHistorico,
                        fin
                );

        Map<Long, Map<LocalDate, EstadoProgramacion>> historicoPorAgente =
                construirHistorico(historico);

        // ========================================================
        // NOVEDADES
        // ========================================================

        Map<Long, Map<LocalDate, NovedadProgramacionRequest>> novedades =
                construirNovedades(
                        request,
                        agentesPorId,
                        inicio,
                        fin
                );

        // ========================================================
        // PROPUESTA
        // ========================================================

        Map<Long, Map<LocalDate, EstadoProgramacion>> propuesta =
                new LinkedHashMap<>();

        Map<Long, Map<LocalDate, Boolean>> patronLaboral =
                new HashMap<>();

        List<ConflictoProgramacionResponse> conflictos =
                new ArrayList<>();

        // ========================================================
        // FULL TIME
        // ========================================================

        List<ProgramacionSecuenciaAgente> fullTime =
                secuencias.stream()
                        .filter(
                                s ->
                                        s.getGrupo() != null
                                                && s.getGrupo()
                                                != GrupoProgramacion.PART_TIME
                        )
                        .sorted(
                                Comparator
                                        .comparing(
                                                ProgramacionSecuenciaAgente::getGrupo,
                                                Comparator.nullsLast(
                                                        Comparator.naturalOrder()
                                                )
                                        )
                                        .thenComparingInt(
                                                (ProgramacionSecuenciaAgente s) ->
                                                        Optional.ofNullable(
                                                                        s.getOrden()
                                                                )
                                                                .orElse(
                                                                        Integer.MAX_VALUE
                                                                )
                                        )
                                        .thenComparingInt(
                                                (ProgramacionSecuenciaAgente s) ->
                                                        Optional.ofNullable(
                                                                        s.getAgente()
                                                                                .getCodigo()
                                                                )
                                                                .orElse(
                                                                        Integer.MAX_VALUE
                                                                )
                                        )
                        )
                        .toList();

        /*
         * Posición 0..7 correspondiente al primer día del mes.
         */
        Map<Long, Integer> posicionInicial =
                determinarPosicionesFullTime(
                        fullTime,
                        inicio,
                        historicoPorAgente,
                        conflictos
                );

        // ========================================================
        // CONSTRUIR CICLO 6x2
        // ========================================================

        for (ProgramacionSecuenciaAgente secuencia : fullTime) {

            Trabajador agente = secuencia.getAgente();

            Integer posicion =
                    posicionInicial.get(agente.getId());

            if (posicion == null) {
                continue;
            }

            Map<LocalDate, Boolean> patron =
                    new LinkedHashMap<>();

            Map<LocalDate, EstadoProgramacion> estados =
                    new LinkedHashMap<>();

            Map<LocalDate, NovedadProgramacionRequest> novedadesAgente =
                    novedades.getOrDefault(
                            agente.getId(),
                            Map.of()
                    );

            int posicionActual = posicion;

            for (
                    LocalDate fecha = inicio;
                    !fecha.isAfter(fin);
                    fecha = fecha.plusDays(1)
            ) {

                boolean trabaja =
                        posicionActual < DIAS_TRABAJO;

                patron.put(fecha, trabaja);

                NovedadProgramacionRequest novedad =
                        novedadesAgente.get(fecha);

                if (novedad != null) {

                    /*
                     * Overlay. El ciclo continúa internamente.
                     */
                    estados.put(
                            fecha,
                            novedad.estado()
                    );

                } else if (trabaja) {

                    /*
                     * null = laboral todavía sin turno.
                     */
                    estados.put(
                            fecha,
                            null
                    );

                } else {

                    estados.put(
                            fecha,
                            EstadoProgramacion.D
                    );
                }

                posicionActual =
                        (posicionActual + 1)
                                % TAMANO_CICLO;
            }

            patronLaboral.put(
                    agente.getId(),
                    patron
            );

            propuesta.put(
                    agente.getId(),
                    estados
            );
        }

        // ========================================================
        // AGENTES SIN SECUENCIA
        // ========================================================

        for (Trabajador agente : agentes) {

            if (!secuenciaPorAgente.containsKey(agente.getId())) {

                conflictos.add(
                        warning(
                                "AGENTE_SIN_SECUENCIA",
                                agente,
                                null,
                                "El agente activo no tiene una secuencia configurada."
                        )
                );
            }
        }

        // ========================================================
        // CONTADORES C
        // ========================================================

        Map<Long, Map<LocalDate, Integer>> cPorBloque =
                inicializarContadoresCFullTime(
                        fullTime,
                        inicio,
                        posicionInicial,
                        historicoPorAgente
                );

        Map<Long, Integer> totalC =
                inicializarTotalC(
                        fullTime,
                        inicio,
                        historicoPorAgente
                );

        // ========================================================
        // PROTECCIÓN SEMANAL DE TURNO C POR SECUENCIA
        // ========================================================

        Map<String, Long> protegidoSinCPorSecuenciaSemana =
                construirProteccionSemanalSinC(
                        fullTime,
                        inicio,
                        fin,
                        historicoPorAgente,
                        excepcionPorAgente,
                        conflictos
                );

        // ========================================================
        // ASIGNAR FULL TIME
        // ========================================================

        asignarFullTime(
                request,
                inicio,
                fin,
                fullTime,
                propuesta,
                patronLaboral,
                posicionInicial,
                novedades,
                excepcionPorAgente,
                historicoPorAgente,
                cPorBloque,
                totalC,
                protegidoSinCPorSecuenciaSemana,
                conflictos
        );

        // ========================================================
        // PART TIME
        // ========================================================

        List<ProgramacionSecuenciaAgente> partTime =
                secuencias.stream()
                        .filter(
                                s ->
                                        s.getGrupo()
                                                == GrupoProgramacion.PART_TIME
                        )
                        .sorted(
                                Comparator
                                        .comparingInt(
                                                (ProgramacionSecuenciaAgente s) ->
                                                        Optional.ofNullable(
                                                                        s.getOrden()
                                                                )
                                                                .orElse(
                                                                        Integer.MAX_VALUE
                                                                )
                                        )
                                        .thenComparingInt(
                                                (ProgramacionSecuenciaAgente s) ->
                                                        Optional.ofNullable(
                                                                        s.getAgente()
                                                                                .getCodigo()
                                                                )
                                                                .orElse(
                                                                        Integer.MAX_VALUE
                                                                )
                                        )
                        )
                        .toList();

        inicializarPartTime(
                partTime,
                inicio,
                fin,
                propuesta,
                novedades
        );

        asignarPartTime(
                request,
                inicio,
                fin,
                partTime,
                propuesta,
                novedades,
                excepcionPorAgente,
                historicoPorAgente,
                conflictos
        );

        validarProteccionSemanalSinC(
                fullTime,
                inicio,
                fin,
                propuesta,
                protegidoSinCPorSecuenciaSemana,
                conflictos
        );

        // ========================================================
        // COBERTURA FINAL
        // ========================================================

        List<CoberturaDiaResponse> cobertura =
                calcularCobertura(
                        request,
                        inicio,
                        fin,
                        propuesta
                );

        for (CoberturaDiaResponse dia : cobertura) {

            if (
                    dia.deficitA() > 0
                            || dia.deficitB() > 0
                            || dia.deficitC() > 0
            ) {

                conflictos.add(
                        new ConflictoProgramacionResponse(
                                "DEFICIT_COBERTURA",
                                "WARNING",
                                null,
                                null,
                                null,
                                dia.fecha(),
                                "Cobertura insuficiente. "
                                        + "A: -" + dia.deficitA()
                                        + ", B: -" + dia.deficitB()
                                        + ", C: -" + dia.deficitC()
                        )
                );
            }

            if (
                    dia.excesoA() > 0
                            || dia.excesoB() > 0
                            || dia.excesoC() > 0
            ) {

                conflictos.add(
                        new ConflictoProgramacionResponse(
                                "SOBRECOBERTURA",
                                "INFO",
                                null,
                                null,
                                null,
                                dia.fecha(),
                                "Existe sobrecobertura. "
                                        + "A: +" + dia.excesoA()
                                        + ", B: +" + dia.excesoB()
                                        + ", C: +" + dia.excesoC()
                        )
                );
            }
        }

        // ========================================================
        // RESPUESTA
        // ========================================================

        List<ProgramacionAgentePropuesta> respuestaAgentes =
                construirRespuestaAgentes(
                        secuencias,
                        propuesta,
                        patronLaboral,
                        excepcionPorAgente,
                        novedades
                );

        return new ProgramacionPropuestaResponse(
                plaza.getId(),
                String.valueOf(plaza.getCodigo()),
                request.anio(),
                request.mes(),
                false,
                respuestaAgentes,
                cobertura,
                conflictos
        );
    }

    // ============================================================
    // RECONSTRUCCIÓN CICLO
    // ============================================================

    private Map<Long, Integer> determinarPosicionesFullTime(
            List<ProgramacionSecuenciaAgente> fullTime,
            LocalDate inicio,
            Map<Long, Map<LocalDate, EstadoProgramacion>> historico,
            List<ConflictoProgramacionResponse> conflictos
    ) {

        Map<Long, Set<Integer>> candidatos =
                new HashMap<>();

        for (ProgramacionSecuenciaAgente secuencia : fullTime) {

            Long trabajadorId =
                    secuencia.getAgente().getId();

            Set<Integer> posibles =
                    posicionesCompatibles(
                            inicio,
                            historico.getOrDefault(
                                    trabajadorId,
                                    Map.of()
                            )
                    );

            candidatos.put(
                    trabajadorId,
                    posibles
            );
        }

        Map<GrupoProgramacion, Integer> anclaGrupo =
                new EnumMap<>(
                        GrupoProgramacion.class
                );

        List<GrupoProgramacion> grupos =
                List.of(
                        GrupoProgramacion.SECUENCIA_1,
                        GrupoProgramacion.SECUENCIA_2,
                        GrupoProgramacion.SECUENCIA_3,
                        GrupoProgramacion.SECUENCIA_4
                );

        for (GrupoProgramacion grupo : grupos) {

            List<ProgramacionSecuenciaAgente> miembrosGrupo =
                    fullTime.stream()
                            .filter(s -> s.getGrupo() == grupo)
                            .toList();

            if (miembrosGrupo.isEmpty()) {
                continue;
            }

            /*
             * Primero buscamos una posición común compatible con TODO el grupo.
             * Esto es importante cuando el historial no alcanza para dejar una
             * única posición por trabajador: antes el generador podía quedarse
             * sin ancla y terminar omitiendo a los Full Time de la propuesta.
             *
             * Si varias posiciones siguen siendo posibles, elegimos una de forma
             * determinista entre las compatibles y dejamos una advertencia. No se
             * inventa una posición incompatible con el historial observado.
             */
            Set<Integer> interseccion = null;

            for (ProgramacionSecuenciaAgente miembro : miembrosGrupo) {
                Set<Integer> posibles = candidatos.getOrDefault(
                        miembro.getAgente().getId(),
                        Set.of()
                );

                if (posibles.isEmpty()) {
                    continue;
                }

                if (interseccion == null) {
                    interseccion = new LinkedHashSet<>(posibles);
                } else {
                    interseccion.retainAll(posibles);
                }
            }

            if (interseccion != null && !interseccion.isEmpty()) {
                int ancla = interseccion.stream().min(Integer::compareTo).orElseThrow();
                anclaGrupo.put(grupo, ancla);

                if (interseccion.size() > 1) {
                    conflictos.add(
                            warning(
                                    "CICLO_6X2_INFERIDO_GRUPO",
                                    miembrosGrupo.get(0).getAgente(),
                                    inicio,
                                    "El historial permite varias posiciones 6x2 para "
                                            + grupo + ". Se usó la posición compatible "
                                            + ancla + " para mantener activa la programación FT."
                            )
                    );
                }
                continue;
            }

            /*
             * Si no existe intersección común, conservamos la detección de
             * inconsistencias. En este caso no forzamos una posición que pueda
             * contradecir el historial real.
             */
            Set<Integer> posicionesExactas = miembrosGrupo.stream()
                    .map(s -> candidatos.get(s.getAgente().getId()))
                    .filter(Objects::nonNull)
                    .filter(set -> set.size() == 1)
                    .map(set -> set.iterator().next())
                    .collect(Collectors.toSet());

            if (posicionesExactas.size() == 1) {
                anclaGrupo.put(grupo, posicionesExactas.iterator().next());
            } else {
                for (ProgramacionSecuenciaAgente secuencia : miembrosGrupo) {
                    conflictos.add(
                            warning(
                                    "SECUENCIA_6X2_INCONSISTENTE",
                                    secuencia.getAgente(),
                                    inicio,
                                    "No existe una posición 6x2 común compatible entre los integrantes de "
                                            + grupo + "."
                            )
                    );
                }
            }
        }

        Map<Long, Integer> resultado =
                new HashMap<>();

        for (ProgramacionSecuenciaAgente secuencia : fullTime) {

            Trabajador agente =
                    secuencia.getAgente();

            Set<Integer> posibles =
                    candidatos.getOrDefault(
                            agente.getId(),
                            Set.of()
                    );

            Integer posicion = null;

            if (posibles.size() == 1) {

                posicion =
                        posibles.iterator().next();

            } else {

                Integer ancla =
                        anclaGrupo.get(
                                secuencia.getGrupo()
                        );

                if (
                        ancla != null
                                && posibles.contains(ancla)
                ) {

                    posicion = ancla;
                }
            }

            if (posicion != null) {

                resultado.put(
                        agente.getId(),
                        posicion
                );

            } else {

                conflictos.add(
                        warning(
                                "CICLO_6X2_SIN_HISTORICO",
                                agente,
                                inicio,
                                "No existe historial suficiente para determinar "
                                        + "de forma segura la posición del ciclo 6x2."
                        )
                );
            }
        }

        return resultado;
    }

    private Set<Integer> posicionesCompatibles(
            LocalDate inicio,
            Map<LocalDate, EstadoProgramacion> historico
    ) {

        LocalDate desde =
                inicio.minusDays(32);

        boolean tieneEvidenciaEstructural =
                historico.entrySet()
                        .stream()
                        .filter(
                                e ->
                                        !e.getKey().isBefore(desde)
                                                && e.getKey().isBefore(inicio)
                        )
                        .map(Map.Entry::getValue)
                        .anyMatch(
                                estado ->
                                        estado == EstadoProgramacion.A
                                                || estado == EstadoProgramacion.B
                                                || estado == EstadoProgramacion.C
                                                || estado == EstadoProgramacion.D
                        );

        if (!tieneEvidenciaEstructural) {

            Set<Integer> todos =
                    new LinkedHashSet<>();

            for (int i = 0; i < TAMANO_CICLO; i++) {
                todos.add(i);
            }

            return todos;
        }

        Set<Integer> resultado =
                new LinkedHashSet<>();

        for (
                int candidato = 0;
                candidato < TAMANO_CICLO;
                candidato++
        ) {

            boolean compatible = true;

            for (
                    Map.Entry<LocalDate, EstadoProgramacion> entry :
                    historico.entrySet()
            ) {

                LocalDate fecha =
                        entry.getKey();

                if (
                        fecha.isBefore(desde)
                                || !fecha.isBefore(inicio)
                ) {
                    continue;
                }

                EstadoProgramacion observado =
                        entry.getValue();

                if (
                        observado == null
                                || esNovedad(observado)
                ) {
                    continue;
                }

                if (
                        observado != EstadoProgramacion.A
                                && observado != EstadoProgramacion.B
                                && observado != EstadoProgramacion.C
                                && observado != EstadoProgramacion.D
                ) {
                    continue;
                }

                long diferencia =
                        ChronoUnit.DAYS.between(
                                inicio,
                                fecha
                        );

                int posicionFecha =
                        Math.floorMod(
                                candidato
                                        + (int) diferencia,
                                TAMANO_CICLO
                        );

                boolean esperadoTrabaja =
                        posicionFecha < DIAS_TRABAJO;

                boolean observadoTrabaja =
                        observado.esOperativo();

                if (esperadoTrabaja != observadoTrabaja) {

                    compatible = false;
                    break;
                }
            }

            if (compatible) {
                resultado.add(candidato);
            }
        }

        return resultado;
    }

    // ============================================================
    // FULL TIME
    // ============================================================

    private void asignarFullTime(
            GenerarProgramacionRequest request,
            LocalDate inicio,
            LocalDate fin,
            List<ProgramacionSecuenciaAgente> fullTime,
            Map<Long, Map<LocalDate, EstadoProgramacion>> propuesta,
            Map<Long, Map<LocalDate, Boolean>> patronLaboral,
            Map<Long, Integer> posicionInicial,
            Map<Long, Map<LocalDate, NovedadProgramacionRequest>> novedades,
            Map<Long, AgenteProgramacionExcepcion> excepciones,
            Map<Long, Map<LocalDate, EstadoProgramacion>> historico,
            Map<Long, Map<LocalDate, Integer>> cPorBloque,
            Map<Long, Integer> totalC,
            Map<String, Long> protegidoSinCPorSecuenciaSemana,
            List<ConflictoProgramacionResponse> conflictos
    ) {

        for (
                LocalDate fecha = inicio;
                !fecha.isAfter(fin);
                fecha = fecha.plusDays(1)
        ) {

            CoberturaTurnosRequest requerida =
                    coberturaParaFecha(
                            request,
                            fecha
                    );

            List<ProgramacionSecuenciaAgente> disponibles =
                    new ArrayList<>();

            for (ProgramacionSecuenciaAgente secuencia : fullTime) {

                Trabajador agente =
                        secuencia.getAgente();

                Map<LocalDate, Boolean> patron =
                        patronLaboral.get(
                                agente.getId()
                        );

                if (
                        patron == null
                                || !Boolean.TRUE.equals(
                                patron.get(fecha)
                        )
                ) {
                    continue;
                }

                if (
                        novedades
                                .getOrDefault(
                                        agente.getId(),
                                        Map.of()
                                )
                                .containsKey(fecha)
                ) {
                    continue;
                }

                disponibles.add(secuencia);
            }

            /*
             * C se intenta primero, PERO solamente pueden
             * recibirlo quienes estén en posición 5 o 6.
             */
            asignarTurnoFullTime(
                    fecha,
                    EstadoProgramacion.C,
                    requerida.c(),
                    disponibles,
                    propuesta,
                    excepciones,
                    posicionInicial,
                    inicio,
                    historico,
                    cPorBloque,
                    totalC,
                    protegidoSinCPorSecuenciaSemana
            );

            asignarTurnoFullTime(
                    fecha,
                    EstadoProgramacion.B,
                    requerida.b(),
                    disponibles,
                    propuesta,
                    excepciones,
                    posicionInicial,
                    inicio,
                    historico,
                    cPorBloque,
                    totalC,
                    protegidoSinCPorSecuenciaSemana
            );

            asignarTurnoFullTime(
                    fecha,
                    EstadoProgramacion.A,
                    requerida.a(),
                    disponibles,
                    propuesta,
                    excepciones,
                    posicionInicial,
                    inicio,
                    historico,
                    cPorBloque,
                    totalC,
                    protegidoSinCPorSecuenciaSemana
            );

            /*
             * Todo FT que estructuralmente trabaja debe recibir
             * algún turno compatible.
             */
            for (ProgramacionSecuenciaAgente secuencia : disponibles) {

                Trabajador agente =
                        secuencia.getAgente();

                EstadoProgramacion actual =
                        propuesta
                                .get(agente.getId())
                                .get(fecha);

                if (actual != null) {
                    continue;
                }

                EstadoProgramacion turno =
                        elegirTurnoResidualFullTime(
                                request,
                                fecha,
                                agente,
                                propuesta,
                                excepciones.get(
                                        agente.getId()
                                ),
                                posicionInicial,
                                inicio,
                                historico,
                                cPorBloque,
                                totalC,
                                secuencia,
                                protegidoSinCPorSecuenciaSemana
                        );

                if (turno != null) {

                    asignarEstado(
                            agente.getId(),
                            fecha,
                            turno,
                            propuesta
                    );

                    if (turno == EstadoProgramacion.C) {

                        registrarC(
                                agente.getId(),
                                fecha,
                                inicio,
                                posicionInicial,
                                cPorBloque,
                                totalC
                        );
                    }

                } else {

                    propuesta
                            .get(agente.getId())
                            .put(
                                    fecha,
                                    null
                            );

                    conflictos.add(
                            error(
                                    "SIN_TURNO_COMPATIBLE",
                                    agente,
                                    fecha,
                                    "El agente debe trabajar según su ciclo 6x2, "
                                            + "pero no existe un turno compatible "
                                            + "con las reglas y restricciones."
                            )
                    );
                }
            }
        }
    }

    private void asignarTurnoFullTime(
            LocalDate fecha,
            EstadoProgramacion turno,
            int requerido,
            List<ProgramacionSecuenciaAgente> disponibles,
            Map<Long, Map<LocalDate, EstadoProgramacion>> propuesta,
            Map<Long, AgenteProgramacionExcepcion> excepciones,
            Map<Long, Integer> posicionInicial,
            LocalDate inicioMes,
            Map<Long, Map<LocalDate, EstadoProgramacion>> historico,
            Map<Long, Map<LocalDate, Integer>> cPorBloque,
            Map<Long, Integer> totalC,
            Map<String, Long> protegidoSinCPorSecuenciaSemana
    ) {

        int actual =
                contar(
                        propuesta,
                        fecha,
                        turno
                );

        int faltan =
                Math.max(
                        0,
                        requerido - actual
                );

        if (faltan == 0) {
            return;
        }

        List<ProgramacionSecuenciaAgente> candidatos =
                disponibles.stream()
                        .filter(
                                (ProgramacionSecuenciaAgente s) ->
                                        propuesta
                                                .get(
                                                        s.getAgente().getId()
                                                )
                                                .get(fecha)
                                                == null
                        )
                        .filter(
                                (ProgramacionSecuenciaAgente s) ->
                                        permite(
                                                excepciones.get(
                                                        s.getAgente().getId()
                                                ),
                                                turno
                                        )
                        )

                        /*
                         * NUEVA REGLA:
                         * C únicamente en posiciones laborales 5/6.
                         */
                        .filter(
                                (ProgramacionSecuenciaAgente s) ->
                                        turno != EstadoProgramacion.C
                                                || puedeAsignarCFullTime(
                                                s.getAgente().getId(),
                                                fecha,
                                                inicioMes,
                                                posicionInicial,
                                                cPorBloque
                                        )
                        )

                        /*
                         * Al menos un FT de cada secuencia queda protegido
                         * de C durante cada semana ISO.
                         */
                        .filter(
                                (ProgramacionSecuenciaAgente s) ->
                                        turno != EstadoProgramacion.C
                                                || !estaProtegidoSinC(
                                                s,
                                                fecha,
                                                protegidoSinCPorSecuenciaSemana
                                        )
                        )

                        /*
                         * Respeta C -> A/B y la progresión A -> B -> C.
                         */
                        .filter(
                                (ProgramacionSecuenciaAgente s) ->
                                        transicionPermitidaFullTime(
                                                s.getAgente().getId(),
                                                fecha,
                                                turno,
                                                propuesta,
                                                historico,
                                                excepciones.get(s.getAgente().getId()),
                                                inicioMes,
                                                posicionInicial
                                        )
                        )
                        .sorted(
                                comparadorCandidatos(
                                        turno,
                                        fecha,
                                        inicioMes,
                                        posicionInicial,
                                        cPorBloque,
                                        totalC,
                                        propuesta,
                                        historico,
                                        excepciones,
                                        protegidoSinCPorSecuenciaSemana
                                )
                        )
                        .toList();

        for (ProgramacionSecuenciaAgente candidato : candidatos) {

            if (faltan <= 0) {
                break;
            }

            Trabajador agente =
                    candidato.getAgente();

            asignarEstado(
                    agente.getId(),
                    fecha,
                    turno,
                    propuesta
            );

            if (turno == EstadoProgramacion.C) {

                registrarC(
                        agente.getId(),
                        fecha,
                        inicioMes,
                        posicionInicial,
                        cPorBloque,
                        totalC
                );
            }

            faltan--;
        }
    }

    // ============================================================
    // REGLA C SOLO POSICIÓN 5/6
    // ============================================================

    private boolean puedeAsignarCFullTime(
            Long trabajadorId,
            LocalDate fecha,
            LocalDate inicioMes,
            Map<Long, Integer> posicionInicial,
            Map<Long, Map<LocalDate, Integer>> cPorBloque
    ) {

        /*
         * Primero validamos posición.
         */
        int posicion =
                posicionCiclo(
                        trabajadorId,
                        fecha,
                        inicioMes,
                        posicionInicial
                );

        /*
         * Índices:
         *
         * 4 = día laboral 5
         * 5 = día laboral 6
         */
        boolean posicionPermiteC =
                posicion == 4
                        || posicion == 5;

        if (!posicionPermiteC) {
            return false;
        }

        /*
         * Segundo: máximo 2 C en el bloque.
         */
        return cantidadCEnBloque(
                trabajadorId,
                fecha,
                inicioMes,
                posicionInicial,
                cPorBloque
        ) < MAX_C_FULL_TIME_BLOQUE;
    }

    private int posicionCiclo(
            Long trabajadorId,
            LocalDate fecha,
            LocalDate inicioMes,
            Map<Long, Integer> posicionInicial
    ) {

        Integer inicial =
                posicionInicial.get(
                        trabajadorId
                );

        if (inicial == null) {
            return -1;
        }

        long delta =
                ChronoUnit.DAYS.between(
                        inicioMes,
                        fecha
                );

        return Math.floorMod(
                inicial + (int) delta,
                TAMANO_CICLO
        );
    }

    // ============================================================
    // REGLAS DE TRANSICIÓN
    // ============================================================

    /**
     * Regla común para FT y PT:
     * después de un C, el día calendario siguiente no puede ser A ni B.
     * Un descanso/novedad intermedia rompe la transición directa.
     */
    private boolean transicionPermitida(
            Long trabajadorId,
            LocalDate fecha,
            EstadoProgramacion turnoNuevo,
            Map<Long, Map<LocalDate, EstadoProgramacion>> propuesta,
            Map<Long, Map<LocalDate, EstadoProgramacion>> historico
    ) {

        EstadoProgramacion ayer =
                estadoDiaAnterior(
                        trabajadorId,
                        fecha,
                        propuesta,
                        historico
                );

        if (ayer == EstadoProgramacion.C) {
            return turnoNuevo != EstadoProgramacion.A
                    && turnoNuevo != EstadoProgramacion.B;
        }

        return true;
    }

    /**
     * Reglas adicionales para Full Time dentro del bloque 6x2:
     *
     * 1. Progresión natural A -> B -> C.
     * 2. Si ya apareció B en el bloque, no regresar a A.
     * 3. Si ya apareció C en el bloque, no regresar a A/B.
     * 4. Si A está permitido, las dos primeras posiciones laborales
     *    se mantienen en A como base del patrón A A B B C C.
     * 5. Las restricciones individuales tienen prioridad. Si A no está
     *    permitido, el bloque puede comenzar directamente en B.
     */
    private boolean transicionPermitidaFullTime(
            Long trabajadorId,
            LocalDate fecha,
            EstadoProgramacion turnoNuevo,
            Map<Long, Map<LocalDate, EstadoProgramacion>> propuesta,
            Map<Long, Map<LocalDate, EstadoProgramacion>> historico,
            AgenteProgramacionExcepcion excepcion,
            LocalDate inicioMes,
            Map<Long, Integer> posicionInicial
    ) {

        if (!transicionPermitida(
                trabajadorId,
                fecha,
                turnoNuevo,
                propuesta,
                historico
        )) {
            return false;
        }

        int posicion = posicionCiclo(
                trabajadorId,
                fecha,
                inicioMes,
                posicionInicial
        );

        if (posicion < 0 || posicion >= DIAS_TRABAJO) {
            return false;
        }

        EstadoProgramacion ultimo = ultimoTurnoOperativoDelBloque(
                trabajadorId,
                fecha,
                inicioMes,
                posicionInicial,
                propuesta,
                historico
        );

        /*
         * Patrón base FT: A A B B C C.
         *
         * Si A está permitido, las dos primeras posiciones laborales
         * deben permanecer en A mientras el bloque todavía no haya
         * avanzado realmente a B/C. Esto evita patrones como:
         *
         * A B B B B B
         *
         * y hace que el inicio normal sea:
         *
         * A A ...
         *
         * Si existe una restricción que impide A, B sí puede ocupar
         * desde el inicio esas posiciones. También respetamos el
         * histórico cuando el bloque comenzó en el mes anterior.
         */
        if (posicion <= 1
                && turnoNuevo == EstadoProgramacion.B
                && permite(excepcion, EstadoProgramacion.A)
                && ultimo != EstadoProgramacion.B
                && ultimo != EstadoProgramacion.C) {
            return false;
        }

        if (ultimo == EstadoProgramacion.C) {
            return turnoNuevo == EstadoProgramacion.C;
        }

        if (ultimo == EstadoProgramacion.B
                && turnoNuevo == EstadoProgramacion.A) {
            return false;
        }

        return true;
    }

    private EstadoProgramacion ultimoTurnoOperativoDelBloque(
            Long trabajadorId,
            LocalDate fecha,
            LocalDate inicioMes,
            Map<Long, Integer> posicionInicial,
            Map<Long, Map<LocalDate, EstadoProgramacion>> propuesta,
            Map<Long, Map<LocalDate, EstadoProgramacion>> historico
    ) {

        int posicion = posicionCiclo(
                trabajadorId,
                fecha,
                inicioMes,
                posicionInicial
        );

        if (posicion <= 0 || posicion >= DIAS_TRABAJO) {
            return null;
        }

        LocalDate inicioBloque = fecha.minusDays(posicion);

        for (LocalDate cursor = fecha.minusDays(1);
             !cursor.isBefore(inicioBloque);
             cursor = cursor.minusDays(1)) {

            EstadoProgramacion estado = estadoEnFecha(
                    trabajadorId,
                    cursor,
                    propuesta,
                    historico
            );

            if (estado != null && estado.esOperativo()) {
                return estado;
            }
        }

        return null;
    }

    private EstadoProgramacion estadoEnFecha(
            Long trabajadorId,
            LocalDate fecha,
            Map<Long, Map<LocalDate, EstadoProgramacion>> propuesta,
            Map<Long, Map<LocalDate, EstadoProgramacion>> historico
    ) {

        Map<LocalDate, EstadoProgramacion> propuestaAgente =
                propuesta.get(trabajadorId);

        if (propuestaAgente != null
                && propuestaAgente.containsKey(fecha)) {
            return propuestaAgente.get(fecha);
        }

        return historico
                .getOrDefault(trabajadorId, Map.of())
                .get(fecha);
    }

    private EstadoProgramacion estadoDiaAnterior(
            Long trabajadorId,
            LocalDate fecha,
            Map<Long, Map<LocalDate, EstadoProgramacion>> propuesta,
            Map<Long, Map<LocalDate, EstadoProgramacion>> historico
    ) {

        return estadoEnFecha(
                trabajadorId,
                fecha.minusDays(1),
                propuesta,
                historico
        );
    }

    // ============================================================
    // COMPARADOR FULL TIME
    // ============================================================

    private Comparator<ProgramacionSecuenciaAgente> comparadorCandidatos(
            EstadoProgramacion turno,
            LocalDate fecha,
            LocalDate inicioMes,
            Map<Long, Integer> posicionInicial,
            Map<Long, Map<LocalDate, Integer>> cPorBloque,
            Map<Long, Integer> totalC,
            Map<Long, Map<LocalDate, EstadoProgramacion>> propuesta,
            Map<Long, Map<LocalDate, EstadoProgramacion>> historico,
            Map<Long, AgenteProgramacionExcepcion> excepciones,
            Map<String, Long> protegidoSinCPorSecuenciaSemana
    ) {

        Comparator<ProgramacionSecuenciaAgente> comparador;

        /*
         * La primera prioridad es parecerse al patrón A A B B C C.
         * Para C, además, si ayer ya hizo C se prioriza mantener el par CC
         * en lugar de repartir C aislados entre distintos trabajadores.
         */
        comparador =
                Comparator
                        .comparingInt(
                                (ProgramacionSecuenciaAgente secuencia) ->
                                        penalizacionPatronObjetivo(
                                                secuencia,
                                                fecha,
                                                turno,
                                                inicioMes,
                                                posicionInicial,
                                                excepciones.get(
                                                        secuencia.getAgente().getId()
                                                ),
                                                protegidoSinCPorSecuenciaSemana
                                        )
                        );

        if (turno == EstadoProgramacion.C) {
            comparador = comparador
                    .thenComparingInt(
                            (ProgramacionSecuenciaAgente secuencia) ->
                                    estadoDiaAnterior(
                                            secuencia.getAgente().getId(),
                                            fecha,
                                            propuesta,
                                            historico
                                    ) == EstadoProgramacion.C ? 0 : 1
                    )
                    .thenComparingInt(
                            (ProgramacionSecuenciaAgente secuencia) ->
                                    cantidadCEnBloque(
                                            secuencia.getAgente().getId(),
                                            fecha,
                                            inicioMes,
                                            posicionInicial,
                                            cPorBloque
                                    )
                    )
                    .thenComparingInt(
                            (ProgramacionSecuenciaAgente secuencia) ->
                                    totalC.getOrDefault(
                                            secuencia.getAgente().getId(),
                                            0
                                    )
                    );
        }

        return comparador
                .thenComparingInt(
                        (ProgramacionSecuenciaAgente s) ->
                                Optional.ofNullable(
                                                s.getOrden()
                                        )
                                        .orElse(
                                                Integer.MAX_VALUE
                                        )
                )
                .thenComparingInt(
                        (ProgramacionSecuenciaAgente s) ->
                                Optional.ofNullable(
                                                s.getAgente().getCodigo()
                                        )
                                        .orElse(
                                                Integer.MAX_VALUE
                                        )
                );
    }


    /**
     * Penalización del patrón objetivo A A B B C C.
     * Menor valor = mayor prioridad.
     *
     * Si el trabajador está protegido de C esa semana, sus posiciones
     * 5/6 prefieren B para producir variantes como A A B B B B.
     */
    private int penalizacionPatronObjetivo(
            ProgramacionSecuenciaAgente secuencia,
            LocalDate fecha,
            EstadoProgramacion turno,
            LocalDate inicioMes,
            Map<Long, Integer> posicionInicial,
            AgenteProgramacionExcepcion excepcion,
            Map<String, Long> protegidoSinCPorSecuenciaSemana
    ) {
        int posicion = posicionCiclo(
                secuencia.getAgente().getId(),
                fecha,
                inicioMes,
                posicionInicial
        );

        if (posicion < 0 || posicion >= DIAS_TRABAJO) {
            return 100;
        }

        boolean protegido = estaProtegidoSinC(
                secuencia, fecha, protegidoSinCPorSecuenciaSemana
        );

        EstadoProgramacion ideal;
        if (posicion <= 1) {
            ideal = permite(excepcion, EstadoProgramacion.A)
                    ? EstadoProgramacion.A
                    : EstadoProgramacion.B;
        } else if (posicion <= 3) {
            ideal = EstadoProgramacion.B;
        } else {
            ideal = protegido
                    ? EstadoProgramacion.B
                    : EstadoProgramacion.C;
        }

        if (turno == ideal) {
            return 0;
        }

        // Alternativas que todavía conservan una progresión razonable.
        if (posicion <= 1 && turno == EstadoProgramacion.B) return 20;
        if (posicion >= 2 && posicion <= 3 && turno == EstadoProgramacion.A) return 20;
        if (posicion >= 4 && turno == EstadoProgramacion.B) return 15;
        if (posicion >= 4 && turno == EstadoProgramacion.A) return 30;
        if (posicion >= 4 && turno == EstadoProgramacion.C) return protegido ? 100 : 0;

        return 50;
    }

    private boolean estaProtegidoSinC(
            ProgramacionSecuenciaAgente secuencia,
            LocalDate fecha,
            Map<String, Long> protegidoSinCPorSecuenciaSemana
    ) {
        if (secuencia == null || secuencia.getGrupo() == null) {
            return false;
        }
        Long protegido = protegidoSinCPorSecuenciaSemana.get(
                claveProteccion(secuencia.getGrupo(), fecha)
        );
        return Objects.equals(protegido, secuencia.getAgente().getId());
    }

    private String claveProteccion(
            GrupoProgramacion grupo,
            LocalDate fecha
    ) {
        return grupo.name() + "|" + claveSemana(fecha);
    }

    /**
     * Selecciona de forma rotativa al menos un FT por secuencia y semana
     * que no recibirá C. Si la semana cruza desde el mes anterior, solo
     * se elige alguien que no tenga ya un C histórico en esa semana.
     */
    private Map<String, Long> construirProteccionSemanalSinC(
            List<ProgramacionSecuenciaAgente> fullTime,
            LocalDate inicio,
            LocalDate fin,
            Map<Long, Map<LocalDate, EstadoProgramacion>> historico,
            Map<Long, AgenteProgramacionExcepcion> excepciones,
            List<ConflictoProgramacionResponse> conflictos
    ) {
        Map<String, Long> resultado = new HashMap<>();

        Map<GrupoProgramacion, List<ProgramacionSecuenciaAgente>> porGrupo =
                fullTime.stream()
                        .filter(s -> s.getGrupo() != null
                                && s.getGrupo() != GrupoProgramacion.PART_TIME)
                        .collect(Collectors.groupingBy(
                                ProgramacionSecuenciaAgente::getGrupo,
                                () -> new EnumMap<>(GrupoProgramacion.class),
                                Collectors.toList()
                        ));

        LocalDate semana = inicio.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate ultimaSemana = fin.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        while (!semana.isAfter(ultimaSemana)) {
            LocalDate lunes = semana;
            LocalDate domingo = lunes.plusDays(6);

            for (Map.Entry<GrupoProgramacion, List<ProgramacionSecuenciaAgente>> entry : porGrupo.entrySet()) {
                GrupoProgramacion grupo = entry.getKey();
                List<ProgramacionSecuenciaAgente> miembros = entry.getValue().stream()
                        .sorted(Comparator
                                .comparingInt((ProgramacionSecuenciaAgente s) ->
                                        Optional.ofNullable(s.getOrden()).orElse(Integer.MAX_VALUE))
                                .thenComparingInt(s ->
                                        Optional.ofNullable(s.getAgente().getCodigo()).orElse(Integer.MAX_VALUE)))
                        .toList();

                if (miembros.isEmpty()) continue;

                List<ProgramacionSecuenciaAgente> elegibles = miembros.stream()
                        .filter(s -> !tuvoCEnRangoHistorico(
                                s.getAgente().getId(), lunes, inicio.minusDays(1), historico))
                        .toList();

                if (elegibles.isEmpty()) {
                    ProgramacionSecuenciaAgente referencia = miembros.get(0);
                    conflictos.add(warning(
                            "SIN_C_SEMANAL_NO_GARANTIZABLE",
                            referencia.getAgente(),
                            lunes.isBefore(inicio) ? inicio : lunes,
                            "La semana " + claveSemana(lunes) + " de " + grupo
                                    + " ya contiene C históricos para todos sus integrantes; "
                                    + "no se puede garantizar un agente sin C en toda la semana."
                    ));
                    continue;
                }

                // Primero favorecemos a quien ya tenga restricción de C.
                List<ProgramacionSecuenciaAgente> sinCPermitido = elegibles.stream()
                        .filter(s -> !permite(
                                excepciones.get(s.getAgente().getId()),
                                EstadoProgramacion.C))
                        .toList();
                List<ProgramacionSecuenciaAgente> bolsa =
                        sinCPermitido.isEmpty() ? elegibles : sinCPermitido;

                WeekFields wf = WeekFields.ISO;
                int numeroSemana = lunes.get(wf.weekOfWeekBasedYear());
                int indice = Math.floorMod(numeroSemana + grupo.ordinal(), bolsa.size());
                ProgramacionSecuenciaAgente elegido = bolsa.get(indice);

                resultado.put(claveProteccion(grupo, lunes), elegido.getAgente().getId());
            }

            semana = semana.plusWeeks(1);
        }

        return resultado;
    }

    private boolean tuvoCEnRangoHistorico(
            Long trabajadorId,
            LocalDate desde,
            LocalDate hasta,
            Map<Long, Map<LocalDate, EstadoProgramacion>> historico
    ) {
        if (hasta.isBefore(desde)) return false;
        Map<LocalDate, EstadoProgramacion> mapa = historico.getOrDefault(trabajadorId, Map.of());
        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            if (mapa.get(fecha) == EstadoProgramacion.C) return true;
        }
        return false;
    }

    // ============================================================
    // TURNO RESIDUAL FULL TIME
    // ============================================================

    private EstadoProgramacion elegirTurnoResidualFullTime(
            GenerarProgramacionRequest request,
            LocalDate fecha,
            Trabajador agente,
            Map<Long, Map<LocalDate, EstadoProgramacion>> propuesta,
            AgenteProgramacionExcepcion excepcion,
            Map<Long, Integer> posicionInicial,
            LocalDate inicioMes,
            Map<Long, Map<LocalDate, EstadoProgramacion>> historico,
            Map<Long, Map<LocalDate, Integer>> cPorBloque,
            Map<Long, Integer> totalC,
            ProgramacionSecuenciaAgente secuencia,
            Map<String, Long> protegidoSinCPorSecuenciaSemana
    ) {

        CoberturaTurnosRequest requerida =
                coberturaParaFecha(
                        request,
                        fecha
                );

        List<TurnoResidual> candidatos =
                new ArrayList<>();

        /*
         * A solamente si:
         *
         * - La excepción lo permite.
         * - Respeta la progresión del bloque A -> B -> C.
         */
        if (
                permite(
                        excepcion,
                        EstadoProgramacion.A
                )
                        && transicionPermitidaFullTime(
                        agente.getId(),
                        fecha,
                        EstadoProgramacion.A,
                        propuesta,
                        historico,
                        excepcion,
                        inicioMes,
                        posicionInicial
                )
        ) {

            candidatos.add(
                    new TurnoResidual(
                            EstadoProgramacion.A,
                            excesoSiAsigno(
                                    propuesta,
                                    fecha,
                                    EstadoProgramacion.A,
                                    requerida.a()
                            ),
                            penalizacionPatronObjetivo(
                                    secuencia, fecha, EstadoProgramacion.A,
                                    inicioMes, posicionInicial, excepcion,
                                    protegidoSinCPorSecuenciaSemana
                            )
                    )
            );
        }

        /*
         * B puede aparecer después de A/B, pero nunca después de C
         * ni como inicio normal del bloque si A está permitido.
         */
        if (
                permite(
                        excepcion,
                        EstadoProgramacion.B
                )
                        && transicionPermitidaFullTime(
                        agente.getId(),
                        fecha,
                        EstadoProgramacion.B,
                        propuesta,
                        historico,
                        excepcion,
                        inicioMes,
                        posicionInicial
                )
        ) {

            candidatos.add(
                    new TurnoResidual(
                            EstadoProgramacion.B,
                            excesoSiAsigno(
                                    propuesta,
                                    fecha,
                                    EstadoProgramacion.B,
                                    requerida.b()
                            ),
                            penalizacionPatronObjetivo(
                                    secuencia, fecha, EstadoProgramacion.B,
                                    inicioMes, posicionInicial, excepcion,
                                    protegidoSinCPorSecuenciaSemana
                            )
                    )
            );
        }

        /*
         * C:
         *
         * - excepción permite C
         * - posición 5/6
         * - máximo 2 C
         */
        if (
                permite(
                        excepcion,
                        EstadoProgramacion.C
                )
                        && !estaProtegidoSinC(
                        secuencia,
                        fecha,
                        protegidoSinCPorSecuenciaSemana
                )
                        && puedeAsignarCFullTime(
                        agente.getId(),
                        fecha,
                        inicioMes,
                        posicionInicial,
                        cPorBloque
                )
                        && transicionPermitidaFullTime(
                        agente.getId(),
                        fecha,
                        EstadoProgramacion.C,
                        propuesta,
                        historico,
                        excepcion,
                        inicioMes,
                        posicionInicial
                )
        ) {

            candidatos.add(
                    new TurnoResidual(
                            EstadoProgramacion.C,
                            excesoSiAsigno(
                                    propuesta,
                                    fecha,
                                    EstadoProgramacion.C,
                                    requerida.c()
                            ),
                            penalizacionPatronObjetivo(
                                    secuencia, fecha, EstadoProgramacion.C,
                                    inicioMes, posicionInicial, excepcion,
                                    protegidoSinCPorSecuenciaSemana
                            )
                                    + totalC.getOrDefault(agente.getId(), 0)
                    )
            );
        }

        return candidatos.stream()
                .min(
                        Comparator
                                .comparingInt(
                                        TurnoResidual::exceso
                                )
                                .thenComparingInt(
                                        TurnoResidual::penalizacion
                                )
                )
                .map(
                        TurnoResidual::estado
                )
                .orElse(null);
    }

    private int excesoSiAsigno(
            Map<Long, Map<LocalDate, EstadoProgramacion>> propuesta,
            LocalDate fecha,
            EstadoProgramacion turno,
            int requerido
    ) {

        int actual =
                contar(
                        propuesta,
                        fecha,
                        turno
                );

        return Math.max(
                0,
                actual + 1 - requerido
        );
    }

    private record TurnoResidual(
            EstadoProgramacion estado,
            int exceso,
            int penalizacion
    ) {
    }

    // ============================================================
    // CONTADORES C
    // ============================================================

    private Map<Long, Map<LocalDate, Integer>>
    inicializarContadoresCFullTime(
            List<ProgramacionSecuenciaAgente> fullTime,
            LocalDate inicioMes,
            Map<Long, Integer> posicionInicial,
            Map<Long, Map<LocalDate, EstadoProgramacion>> historico
    ) {

        Map<Long, Map<LocalDate, Integer>> resultado =
                new HashMap<>();

        for (ProgramacionSecuenciaAgente secuencia : fullTime) {

            Long trabajadorId =
                    secuencia.getAgente().getId();

            Integer posicion =
                    posicionInicial.get(
                            trabajadorId
                    );

            Map<LocalDate, Integer> bloques =
                    new HashMap<>();

            if (posicion == null) {

                resultado.put(
                        trabajadorId,
                        bloques
                );

                continue;
            }

            Map<LocalDate, EstadoProgramacion> historialAgente =
                    historico.getOrDefault(
                            trabajadorId,
                            Map.of()
                    );

            LocalDate desde =
                    inicioMes.minusDays(7);

            for (
                    Map.Entry<LocalDate, EstadoProgramacion> entry :
                    historialAgente.entrySet()
            ) {

                LocalDate fecha =
                        entry.getKey();

                if (
                        fecha.isBefore(desde)
                                || !fecha.isBefore(inicioMes)
                ) {
                    continue;
                }

                if (
                        entry.getValue()
                                != EstadoProgramacion.C
                ) {
                    continue;
                }

                LocalDate inicioBloque =
                        inicioBloque(
                                trabajadorId,
                                fecha,
                                inicioMes,
                                posicionInicial
                        );

                LocalDate finBloqueLaboral =
                        inicioBloque.plusDays(
                                DIAS_TRABAJO - 1L
                        );

                if (
                        !finBloqueLaboral.isBefore(
                                inicioMes
                        )
                ) {

                    bloques.merge(
                            inicioBloque,
                            1,
                            Integer::sum
                    );
                }
            }

            resultado.put(
                    trabajadorId,
                    bloques
            );
        }

        return resultado;
    }

    private Map<Long, Integer> inicializarTotalC(
            List<ProgramacionSecuenciaAgente> fullTime,
            LocalDate inicioMes,
            Map<Long, Map<LocalDate, EstadoProgramacion>> historico
    ) {

        Map<Long, Integer> resultado =
                new HashMap<>();

        LocalDate desde =
                inicioMes.minusDays(31);

        for (ProgramacionSecuenciaAgente secuencia : fullTime) {

            Long trabajadorId =
                    secuencia.getAgente().getId();

            int total =
                    (int) historico
                            .getOrDefault(
                                    trabajadorId,
                                    Map.of()
                            )
                            .entrySet()
                            .stream()
                            .filter(
                                    e ->
                                            !e.getKey()
                                                    .isBefore(desde)
                            )
                            .filter(
                                    e ->
                                            e.getKey()
                                                    .isBefore(inicioMes)
                            )
                            .filter(
                                    e ->
                                            e.getValue()
                                                    == EstadoProgramacion.C
                            )
                            .count();

            resultado.put(
                    trabajadorId,
                    total
            );
        }

        return resultado;
    }

    private int cantidadCEnBloque(
            Long trabajadorId,
            LocalDate fecha,
            LocalDate inicioMes,
            Map<Long, Integer> posicionInicial,
            Map<Long, Map<LocalDate, Integer>> cPorBloque
    ) {

        LocalDate bloque =
                inicioBloque(
                        trabajadorId,
                        fecha,
                        inicioMes,
                        posicionInicial
                );

        return cPorBloque
                .getOrDefault(
                        trabajadorId,
                        Map.of()
                )
                .getOrDefault(
                        bloque,
                        0
                );
    }

    private void registrarC(
            Long trabajadorId,
            LocalDate fecha,
            LocalDate inicioMes,
            Map<Long, Integer> posicionInicial,
            Map<Long, Map<LocalDate, Integer>> cPorBloque,
            Map<Long, Integer> totalC
    ) {

        LocalDate bloque =
                inicioBloque(
                        trabajadorId,
                        fecha,
                        inicioMes,
                        posicionInicial
                );

        cPorBloque
                .computeIfAbsent(
                        trabajadorId,
                        ignored -> new HashMap<>()
                )
                .merge(
                        bloque,
                        1,
                        Integer::sum
                );

        totalC.merge(
                trabajadorId,
                1,
                Integer::sum
        );
    }

    private LocalDate inicioBloque(
            Long trabajadorId,
            LocalDate fecha,
            LocalDate inicioMes,
            Map<Long, Integer> posicionInicial
    ) {

        int posicion =
                posicionCiclo(
                        trabajadorId,
                        fecha,
                        inicioMes,
                        posicionInicial
                );

        if (posicion < 0) {
            return fecha;
        }

        return fecha.minusDays(
                posicion
        );
    }

    private void asignarEstado(
            Long trabajadorId,
            LocalDate fecha,
            EstadoProgramacion estado,
            Map<Long, Map<LocalDate, EstadoProgramacion>> propuesta
    ) {

        propuesta
                .get(trabajadorId)
                .put(
                        fecha,
                        estado
                );
    }

    // ============================================================
    // PART TIME
    // ============================================================

    private void inicializarPartTime(
            List<ProgramacionSecuenciaAgente> partTime,
            LocalDate inicio,
            LocalDate fin,
            Map<Long, Map<LocalDate, EstadoProgramacion>> propuesta,
            Map<Long, Map<LocalDate, NovedadProgramacionRequest>> novedades
    ) {

        for (ProgramacionSecuenciaAgente secuencia : partTime) {

            Trabajador agente =
                    secuencia.getAgente();

            Map<LocalDate, EstadoProgramacion> estados =
                    new LinkedHashMap<>();

            Map<LocalDate, NovedadProgramacionRequest> novedadesAgente =
                    novedades.getOrDefault(
                            agente.getId(),
                            Map.of()
                    );

            for (
                    LocalDate fecha = inicio;
                    !fecha.isAfter(fin);
                    fecha = fecha.plusDays(1)
            ) {

                NovedadProgramacionRequest novedad =
                        novedadesAgente.get(fecha);

                estados.put(
                        fecha,
                        novedad != null
                                ? novedad.estado()
                                : EstadoProgramacion.D
                );
            }

            propuesta.put(
                    agente.getId(),
                    estados
            );
        }
    }

    private void asignarPartTime(
            GenerarProgramacionRequest request,
            LocalDate inicio,
            LocalDate fin,
            List<ProgramacionSecuenciaAgente> partTime,
            Map<Long, Map<LocalDate, EstadoProgramacion>> propuesta,
            Map<Long, Map<LocalDate, NovedadProgramacionRequest>> novedades,
            Map<Long, AgenteProgramacionExcepcion> excepciones,
            Map<Long, Map<LocalDate, EstadoProgramacion>> historico,
            List<ConflictoProgramacionResponse> conflictos
    ) {

        if (partTime.isEmpty()) {
            return;
        }

        Map<Long, Map<String, Integer>> diasSemana =
                inicializarDiasSemanaPartTime(partTime, inicio, historico);

        Map<Long, Integer> diasMes = new HashMap<>();
        for (ProgramacionSecuenciaAgente secuencia : partTime) {
            diasMes.put(secuencia.getAgente().getId(), 0);
        }

        /*
         * FASE 1
         * -------
         * Los PT cubren primero los déficits reales de A/B/C.
         * Se mantiene el tope de 3 mientras se construye la semana.
         */
        for (LocalDate fecha = inicio; !fecha.isAfter(fin); fecha = fecha.plusDays(1)) {

            CoberturaTurnosRequest requerida = coberturaParaFecha(request, fecha);

            cubrirConPartTime(
                    fecha, EstadoProgramacion.C,
                    Math.max(0, requerida.c() - contar(propuesta, fecha, EstadoProgramacion.C)),
                    partTime, propuesta, novedades, excepciones, historico,
                    diasSemana, diasMes
            );

            cubrirConPartTime(
                    fecha, EstadoProgramacion.B,
                    Math.max(0, requerida.b() - contar(propuesta, fecha, EstadoProgramacion.B)),
                    partTime, propuesta, novedades, excepciones, historico,
                    diasSemana, diasMes
            );

            cubrirConPartTime(
                    fecha, EstadoProgramacion.A,
                    Math.max(0, requerida.a() - contar(propuesta, fecha, EstadoProgramacion.A)),
                    partTime, propuesta, novedades, excepciones, historico,
                    diasSemana, diasMes
            );
        }

        /*
         * FASE 2
         * -------
         * Regla obligatoria PT: EXACTAMENTE 3 turnos por semana.
         *
         * Si después de cubrir déficits un PT tiene 0, 1 o 2 turnos,
         * completamos hasta 3 buscando primero fechas/turnos que no
         * produzcan sobrecobertura. Si no existe esa posibilidad,
         * elegimos la alternativa de menor exceso.
         *
         * La primera semana del mes incluye los turnos históricos de
         * los días pertenecientes a esa misma semana que estén en el
         * mes anterior.
         */
        completarTresTurnosPartTime(
                request, inicio, fin, partTime, propuesta, novedades,
                excepciones, historico, diasSemana, diasMes, conflictos
        );

        validarTresTurnosPartTime(
                inicio, fin, partTime, diasSemana, conflictos
        );
    }

    private void cubrirConPartTime(
            LocalDate fecha,
            EstadoProgramacion turno,
            int cantidad,
            List<ProgramacionSecuenciaAgente> partTime,
            Map<Long, Map<LocalDate, EstadoProgramacion>> propuesta,
            Map<Long, Map<LocalDate, NovedadProgramacionRequest>> novedades,
            Map<Long, AgenteProgramacionExcepcion> excepciones,
            Map<Long, Map<LocalDate, EstadoProgramacion>> historico,
            Map<Long, Map<String, Integer>> diasSemana,
            Map<Long, Integer> diasMes
    ) {

        if (cantidad <= 0) return;

        String semana = claveSemana(fecha);

        List<ProgramacionSecuenciaAgente> candidatos = partTime.stream()
                .filter(s -> {
                    Long trabajadorId = s.getAgente().getId();

                    if (novedades.getOrDefault(trabajadorId, Map.of()).containsKey(fecha)) {
                        return false;
                    }

                    if (propuesta.get(trabajadorId).get(fecha) != EstadoProgramacion.D) {
                        return false;
                    }

                    if (!permite(excepciones.get(trabajadorId), turno)) {
                        return false;
                    }

                    if (!transicionPermitidaPartTimeCompleta(
                            trabajadorId, fecha, turno, propuesta, historico)) {
                        return false;
                    }

                    int trabajados = diasSemana
                            .getOrDefault(trabajadorId, Map.of())
                            .getOrDefault(semana, 0);

                    return trabajados < DIAS_PART_TIME_SEMANA;
                })
                .sorted(
                        Comparator
                                .comparingInt((ProgramacionSecuenciaAgente s) ->
                                        diasSemana
                                                .getOrDefault(s.getAgente().getId(), Map.of())
                                                .getOrDefault(semana, 0))
                                .thenComparingInt(s ->
                                        diasMes.getOrDefault(s.getAgente().getId(), 0))
                                .thenComparingInt(s ->
                                        Optional.ofNullable(s.getOrden()).orElse(Integer.MAX_VALUE))
                                .thenComparingInt(s ->
                                        Optional.ofNullable(s.getAgente().getCodigo()).orElse(Integer.MAX_VALUE))
                )
                .toList();

        int asignados = 0;
        for (ProgramacionSecuenciaAgente candidato : candidatos) {
            if (asignados >= cantidad) break;

            Long trabajadorId = candidato.getAgente().getId();
            propuesta.get(trabajadorId).put(fecha, turno);

            diasSemana
                    .computeIfAbsent(trabajadorId, ignored -> new HashMap<>())
                    .merge(semana, 1, Integer::sum);

            diasMes.merge(trabajadorId, 1, Integer::sum);
            asignados++;
        }
    }

    private void completarTresTurnosPartTime(
            GenerarProgramacionRequest request,
            LocalDate inicio,
            LocalDate fin,
            List<ProgramacionSecuenciaAgente> partTime,
            Map<Long, Map<LocalDate, EstadoProgramacion>> propuesta,
            Map<Long, Map<LocalDate, NovedadProgramacionRequest>> novedades,
            Map<Long, AgenteProgramacionExcepcion> excepciones,
            Map<Long, Map<LocalDate, EstadoProgramacion>> historico,
            Map<Long, Map<String, Integer>> diasSemana,
            Map<Long, Integer> diasMes,
            List<ConflictoProgramacionResponse> conflictos
    ) {

        LocalDate lunesInicial = inicio.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lunesFinal = fin.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        for (LocalDate lunes = lunesInicial;
             !lunes.isAfter(lunesFinal);
             lunes = lunes.plusWeeks(1)) {

            LocalDate desdeSemana = lunes.isBefore(inicio) ? inicio : lunes;
            LocalDate domingo = lunes.plusDays(6);
            LocalDate hastaSemana = domingo.isAfter(fin) ? fin : domingo;
            String semana = claveSemana(lunes);

            /*
             * Quien lleva menos turnos se completa primero. Esto evita
             * que un PT consuma todas las fechas útiles antes que otro.
             */
            List<ProgramacionSecuenciaAgente> ordenSemana = partTime.stream()
                    .sorted(
                            Comparator
                                    .comparingInt((ProgramacionSecuenciaAgente s) ->
                                            diasSemana
                                                    .getOrDefault(s.getAgente().getId(), Map.of())
                                                    .getOrDefault(semana, 0))
                                    .thenComparingInt(s ->
                                            diasMes.getOrDefault(s.getAgente().getId(), 0))
                                    .thenComparingInt(s ->
                                            Optional.ofNullable(s.getOrden()).orElse(Integer.MAX_VALUE))
                                    .thenComparingInt(s ->
                                            Optional.ofNullable(s.getAgente().getCodigo()).orElse(Integer.MAX_VALUE))
                    )
                    .toList();

            for (ProgramacionSecuenciaAgente secuencia : ordenSemana) {
                Trabajador agente = secuencia.getAgente();
                Long trabajadorId = agente.getId();

                while (diasSemana
                        .getOrDefault(trabajadorId, Map.of())
                        .getOrDefault(semana, 0) < DIAS_PART_TIME_SEMANA) {

                    CandidatoPartTime candidato = mejorCandidatoPartTime(
                            request,
                            trabajadorId,
                            desdeSemana,
                            hastaSemana,
                            propuesta,
                            novedades,
                            excepciones.get(trabajadorId),
                            historico
                    );

                    if (candidato == null) {
                        break;
                    }

                    propuesta
                            .get(trabajadorId)
                            .put(candidato.fecha(), candidato.turno());

                    diasSemana
                            .computeIfAbsent(trabajadorId, ignored -> new HashMap<>())
                            .merge(semana, 1, Integer::sum);

                    diasMes.merge(trabajadorId, 1, Integer::sum);
                }
            }
        }
    }

    private CandidatoPartTime mejorCandidatoPartTime(
            GenerarProgramacionRequest request,
            Long trabajadorId,
            LocalDate desde,
            LocalDate hasta,
            Map<Long, Map<LocalDate, EstadoProgramacion>> propuesta,
            Map<Long, Map<LocalDate, NovedadProgramacionRequest>> novedades,
            AgenteProgramacionExcepcion excepcion,
            Map<Long, Map<LocalDate, EstadoProgramacion>> historico
    ) {

        List<CandidatoPartTime> candidatos = new ArrayList<>();

        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {

            if (novedades.getOrDefault(trabajadorId, Map.of()).containsKey(fecha)) {
                continue;
            }

            if (propuesta.get(trabajadorId).get(fecha) != EstadoProgramacion.D) {
                continue;
            }

            CoberturaTurnosRequest requerida = coberturaParaFecha(request, fecha);

            for (EstadoProgramacion turno : List.of(
                    EstadoProgramacion.A,
                    EstadoProgramacion.B,
                    EstadoProgramacion.C)) {

                if (!permite(excepcion, turno)) {
                    continue;
                }

                if (!transicionPermitidaPartTimeCompleta(
                        trabajadorId, fecha, turno, propuesta, historico)) {
                    continue;
                }

                int requerido = switch (turno) {
                    case A -> requerida.a();
                    case B -> requerida.b();
                    case C -> requerida.c();
                    default -> 0;
                };

                int actual = contar(propuesta, fecha, turno);
                int deficitAntes = Math.max(0, requerido - actual);
                int excesoDespues = Math.max(0, actual + 1 - requerido);

                candidatos.add(new CandidatoPartTime(
                        fecha,
                        turno,
                        deficitAntes > 0 ? 0 : 1,
                        excesoDespues,
                        prioridadTurnoPartTime(turno)
                ));
            }
        }

        return candidatos.stream()
                .min(
                        Comparator
                                .comparingInt(CandidatoPartTime::cubreDeficit)
                                .thenComparingInt(CandidatoPartTime::exceso)
                                .thenComparingInt(CandidatoPartTime::prioridadTurno)
                                .thenComparing(CandidatoPartTime::fecha)
                )
                .orElse(null);
    }

    /*
     * Preferencia de desempate PT. No es una regla estructural:
     * primero A, luego B y finalmente C. La cobertura y el exceso
     * siempre tienen prioridad sobre este desempate.
     */
    private int prioridadTurnoPartTime(EstadoProgramacion turno) {
        return switch (turno) {
            case A -> 0;
            case B -> 1;
            case C -> 2;
            default -> 9;
        };
    }

    /**
     * Para PT comprobamos ambos lados de la fecha porque la fase de
     * completado puede insertar un turno en un día anterior a otro
     * turno ya asignado durante la fase de cobertura.
     */
    private boolean transicionPermitidaPartTimeCompleta(
            Long trabajadorId,
            LocalDate fecha,
            EstadoProgramacion turnoNuevo,
            Map<Long, Map<LocalDate, EstadoProgramacion>> propuesta,
            Map<Long, Map<LocalDate, EstadoProgramacion>> historico
    ) {

        if (!transicionPermitida(
                trabajadorId, fecha, turnoNuevo, propuesta, historico)) {
            return false;
        }

        EstadoProgramacion manana = estadoEnFecha(
                trabajadorId,
                fecha.plusDays(1),
                propuesta,
                historico
        );

        return turnoNuevo != EstadoProgramacion.C
                || (manana != EstadoProgramacion.A
                && manana != EstadoProgramacion.B);
    }

    private void validarTresTurnosPartTime(
            LocalDate inicio,
            LocalDate fin,
            List<ProgramacionSecuenciaAgente> partTime,
            Map<Long, Map<String, Integer>> diasSemana,
            List<ConflictoProgramacionResponse> conflictos
    ) {

        LocalDate lunesInicial = inicio.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lunesFinal = fin.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        for (LocalDate lunes = lunesInicial;
             !lunes.isAfter(lunesFinal);
             lunes = lunes.plusWeeks(1)) {

            String semana = claveSemana(lunes);
            LocalDate fechaReferencia = lunes.isBefore(inicio) ? inicio : lunes;

            for (ProgramacionSecuenciaAgente secuencia : partTime) {
                Trabajador agente = secuencia.getAgente();

                int total = diasSemana
                        .getOrDefault(agente.getId(), Map.of())
                        .getOrDefault(semana, 0);

                if (total != DIAS_PART_TIME_SEMANA) {
                    conflictos.add(error(
                            "PART_TIME_TURNOS_SEMANA_INCOMPLETOS",
                            agente,
                            fechaReferencia,
                            "El trabajador Part Time debe tener exactamente "
                                    + DIAS_PART_TIME_SEMANA
                                    + " turnos en la semana " + semana
                                    + ", pero solo fue posible asignar " + total + "."
                    ));
                }
            }
        }
    }

    private Map<Long, Map<String, Integer>>
    inicializarDiasSemanaPartTime(
            List<ProgramacionSecuenciaAgente> partTime,
            LocalDate inicio,
            Map<Long, Map<LocalDate, EstadoProgramacion>> historico
    ) {

        Map<Long, Map<String, Integer>> resultado = new HashMap<>();

        LocalDate lunes = inicio.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        for (ProgramacionSecuenciaAgente secuencia : partTime) {

            Long trabajadorId = secuencia.getAgente().getId();
            Map<String, Integer> semanas = new HashMap<>();
            Map<LocalDate, EstadoProgramacion> historial =
                    historico.getOrDefault(trabajadorId, Map.of());

            /*
             * Solo necesitamos precargar los días anteriores al inicio
             * que pertenecen a la misma semana calendario del día 1.
             */
            for (LocalDate fecha = lunes;
                 fecha.isBefore(inicio);
                 fecha = fecha.plusDays(1)) {

                EstadoProgramacion estado = historial.get(fecha);

                if (estado != null && estado.esOperativo()) {
                    semanas.merge(claveSemana(fecha), 1, Integer::sum);
                }
            }

            resultado.put(trabajadorId, semanas);
        }

        return resultado;
    }

    private record CandidatoPartTime(
            LocalDate fecha,
            EstadoProgramacion turno,
            int cubreDeficit,
            int exceso,
            int prioridadTurno
    ) {
    }

    // ============================================================
    // COBERTURA
    // ============================================================

    private List<CoberturaDiaResponse> calcularCobertura(
            GenerarProgramacionRequest request,
            LocalDate inicio,
            LocalDate fin,
            Map<Long, Map<LocalDate, EstadoProgramacion>> propuesta
    ) {

        List<CoberturaDiaResponse> resultado =
                new ArrayList<>();

        for (
                LocalDate fecha = inicio;
                !fecha.isAfter(fin);
                fecha = fecha.plusDays(1)
        ) {

            CoberturaTurnosRequest requerida =
                    coberturaParaFecha(
                            request,
                            fecha
                    );

            int asignadoA =
                    contar(
                            propuesta,
                            fecha,
                            EstadoProgramacion.A
                    );

            int asignadoB =
                    contar(
                            propuesta,
                            fecha,
                            EstadoProgramacion.B
                    );

            int asignadoC =
                    contar(
                            propuesta,
                            fecha,
                            EstadoProgramacion.C
                    );

            resultado.add(
                    new CoberturaDiaResponse(
                            fecha,

                            requerida.a(),
                            requerida.b(),
                            requerida.c(),

                            asignadoA,
                            asignadoB,
                            asignadoC,

                            Math.max(
                                    0,
                                    requerida.a() - asignadoA
                            ),

                            Math.max(
                                    0,
                                    requerida.b() - asignadoB
                            ),

                            Math.max(
                                    0,
                                    requerida.c() - asignadoC
                            ),

                            Math.max(
                                    0,
                                    asignadoA - requerida.a()
                            ),

                            Math.max(
                                    0,
                                    asignadoB - requerida.b()
                            ),

                            Math.max(
                                    0,
                                    asignadoC - requerida.c()
                            ),

                            tipoCobertura(
                                    request,
                                    fecha
                            )
                    )
            );
        }

        return resultado;
    }

    private CoberturaTurnosRequest coberturaParaFecha(
            GenerarProgramacionRequest request,
            LocalDate fecha
    ) {

        /*
         * 1. Día especial.
         */
        if (request.diasEspeciales() != null) {

            Optional<DiaEspecialRequest> especial =
                    request
                            .diasEspeciales()
                            .stream()
                            .filter(
                                    d ->
                                            fecha.equals(
                                                    d.fecha()
                                            )
                            )
                            .findFirst();

            if (especial.isPresent()) {

                DiaEspecialRequest dia =
                        especial.get();

                return new CoberturaTurnosRequest(
                        dia.a(),
                        dia.b(),
                        dia.c()
                );
            }
        }

        /*
         * 2. Domingo.
         */
        if (
                fecha.getDayOfWeek()
                        == DayOfWeek.SUNDAY
                        && request.coberturaDomingo() != null
        ) {

            return request.coberturaDomingo();
        }

        /*
         * 3. Normal.
         */
        return request.coberturaNormal();
    }

    private String tipoCobertura(
            GenerarProgramacionRequest request,
            LocalDate fecha
    ) {

        if (
                request.diasEspeciales() != null
                        && request
                        .diasEspeciales()
                        .stream()
                        .anyMatch(
                                d ->
                                        fecha.equals(
                                                d.fecha()
                                        )
                        )
        ) {

            return "ESPECIAL";
        }

        if (
                fecha.getDayOfWeek()
                        == DayOfWeek.SUNDAY
                        && request.coberturaDomingo() != null
        ) {

            return "DOMINGO";
        }

        return "NORMAL";
    }

    private int contar(
            Map<Long, Map<LocalDate, EstadoProgramacion>> propuesta,
            LocalDate fecha,
            EstadoProgramacion estado
    ) {

        int total = 0;

        for (
                Map<LocalDate, EstadoProgramacion> estados :
                propuesta.values()
        ) {

            if (estados.get(fecha) == estado) {
                total++;
            }
        }

        return total;
    }

    // ============================================================
    // NOVEDADES
    // ============================================================

    private Map<Long, Map<LocalDate, NovedadProgramacionRequest>>
    construirNovedades(
            GenerarProgramacionRequest request,
            Map<Long, Trabajador> agentes,
            LocalDate inicio,
            LocalDate fin
    ) {

        Map<Long, Map<LocalDate, NovedadProgramacionRequest>> resultado =
                new HashMap<>();

        if (
                request.novedades() == null
                        || request.novedades().isEmpty()
        ) {

            return resultado;
        }

        for (
                NovedadProgramacionRequest novedad :
                request.novedades()
        ) {

            Trabajador agente =
                    agentes.get(
                            novedad.trabajadorId()
                    );

            if (agente == null) {

                throw bad(
                        "El trabajador "
                                + novedad.trabajadorId()
                                + " no pertenece a los agentes activos de la plaza."
                );
            }

            if (
                    novedad.hasta()
                            .isBefore(
                                    novedad.desde()
                            )
            ) {

                throw bad(
                        "La fecha final de la novedad no puede ser "
                                + "anterior a la fecha inicial."
                );
            }

            if (!esNovedad(novedad.estado())) {

                throw bad(
                        "Solo V, COM, DM y LIC pueden utilizarse "
                                + "como novedades."
                );
            }

            LocalDate desde =
                    novedad.desde().isBefore(inicio)
                            ? inicio
                            : novedad.desde();

            LocalDate hasta =
                    novedad.hasta().isAfter(fin)
                            ? fin
                            : novedad.hasta();

            if (desde.isAfter(hasta)) {
                continue;
            }

            Map<LocalDate, NovedadProgramacionRequest> porFecha =
                    resultado.computeIfAbsent(
                            agente.getId(),
                            ignored -> new HashMap<>()
                    );

            for (
                    LocalDate fecha = desde;
                    !fecha.isAfter(hasta);
                    fecha = fecha.plusDays(1)
            ) {

                if (porFecha.containsKey(fecha)) {

                    throw bad(
                            "El agente "
                                    + agente.getNombreCompleto()
                                    + " tiene más de una novedad para "
                                    + fecha + "."
                    );
                }

                porFecha.put(
                        fecha,
                        novedad
                );
            }
        }

        return resultado;
    }

    // ============================================================
    // RESPUESTA
    // ============================================================

    private void validarProteccionSemanalSinC(
            List<ProgramacionSecuenciaAgente> fullTime,
            LocalDate inicio,
            LocalDate fin,
            Map<Long, Map<LocalDate, EstadoProgramacion>> propuesta,
            Map<String, Long> protegidoSinCPorSecuenciaSemana,
            List<ConflictoProgramacionResponse> conflictos
    ) {
        Map<Long, ProgramacionSecuenciaAgente> porId = fullTime.stream()
                .collect(Collectors.toMap(s -> s.getAgente().getId(), Function.identity()));

        for (Map.Entry<String, Long> entry : protegidoSinCPorSecuenciaSemana.entrySet()) {
            Long trabajadorId = entry.getValue();
            ProgramacionSecuenciaAgente secuencia = porId.get(trabajadorId);
            if (secuencia == null) continue;

            String semanaClave = entry.getKey().substring(entry.getKey().indexOf('|') + 1);
            boolean tieneC = propuesta.getOrDefault(trabajadorId, Map.of()).entrySet().stream()
                    .anyMatch(e -> !e.getKey().isBefore(inicio)
                            && !e.getKey().isAfter(fin)
                            && claveSemana(e.getKey()).equals(semanaClave)
                            && e.getValue() == EstadoProgramacion.C);

            if (tieneC) {
                conflictos.add(warning(
                        "PROTECCION_SIN_C_INCUMPLIDA",
                        secuencia.getAgente(),
                        null,
                        "El agente protegido de " + secuencia.getGrupo()
                                + " recibió C en la semana " + semanaClave + "."
                ));
            }
        }
    }

    private List<ProgramacionAgentePropuesta> construirRespuestaAgentes(
            List<ProgramacionSecuenciaAgente> secuencias,
            Map<Long, Map<LocalDate, EstadoProgramacion>> propuesta,
            Map<Long, Map<LocalDate, Boolean>> patronLaboral,
            Map<Long, AgenteProgramacionExcepcion> excepciones,
            Map<Long, Map<LocalDate, NovedadProgramacionRequest>> novedades
    ) {

        List<ProgramacionAgentePropuesta> resultado =
                new ArrayList<>();

        List<ProgramacionSecuenciaAgente> ordenadas =
                secuencias.stream()
                        .filter(
                                (ProgramacionSecuenciaAgente s) ->
                                        propuesta.containsKey(
                                                s.getAgente().getId()
                                        )
                        )
                        .sorted(
                                Comparator
                                        .comparing(
                                                ProgramacionSecuenciaAgente::getGrupo,
                                                Comparator.nullsLast(
                                                        Comparator.naturalOrder()
                                                )
                                        )
                                        .thenComparingInt(
                                                (ProgramacionSecuenciaAgente s) ->
                                                        Optional.ofNullable(
                                                                        s.getOrden()
                                                                )
                                                                .orElse(
                                                                        Integer.MAX_VALUE
                                                                )
                                        )
                                        .thenComparingInt(
                                                (ProgramacionSecuenciaAgente s) ->
                                                        Optional.ofNullable(
                                                                        s.getAgente()
                                                                                .getCodigo()
                                                                )
                                                                .orElse(
                                                                        Integer.MAX_VALUE
                                                                )
                                        )
                        )
                        .toList();

        for (ProgramacionSecuenciaAgente secuencia : ordenadas) {

            Trabajador agente =
                    secuencia.getAgente();

            boolean partTime =
                    secuencia.getGrupo()
                            == GrupoProgramacion.PART_TIME;

            Map<LocalDate, EstadoProgramacion> estados =
                    propuesta.get(
                            agente.getId()
                    );

            Map<LocalDate, Boolean> patron =
                    patronLaboral.getOrDefault(
                            agente.getId(),
                            Map.of()
                    );

            Map<LocalDate, NovedadProgramacionRequest> novedadesAgente =
                    novedades.getOrDefault(
                            agente.getId(),
                            Map.of()
                    );

            AgenteProgramacionExcepcion excepcion =
                    excepciones.get(
                            agente.getId()
                    );

            List<ProgramacionDiaPropuesta> dias =
                    new ArrayList<>();

            for (
                    Map.Entry<LocalDate, EstadoProgramacion> entry :
                    estados.entrySet()
            ) {

                LocalDate fecha =
                        entry.getKey();

                EstadoProgramacion estado =
                        entry.getValue();

                NovedadProgramacionRequest novedad =
                        novedadesAgente.get(fecha);

                boolean trabajaEstructural =
                        Boolean.TRUE.equals(
                                patron.get(fecha)
                        );

                EstadoProgramacion estadoCiclo;

                if (partTime) {

                    estadoCiclo =
                            EstadoProgramacion.D;

                } else {

                    estadoCiclo =
                            trabajaEstructural
                                    ? EstadoProgramacion.A
                                    : EstadoProgramacion.D;
                }

                String origen;

                boolean requiereAtencion =
                        false;

                String observacion =
                        null;

                if (novedad != null) {

                    origen =
                            "NOVEDAD";

                    observacion =
                            "Novedad "
                                    + novedad.estado();

                } else if (partTime) {

                    origen =
                            estado != null
                                    && estado.esOperativo()
                                    ? "COBERTURA_PART_TIME"
                                    : "DESCANSO";

                } else if (!trabajaEstructural) {

                    origen =
                            "DESCANSO_6X2";

                } else if (
                        estado != null
                                && estado.esOperativo()
                ) {

                    origen =
                            "ASIGNACION_COBERTURA";

                    if (!permite(excepcion, estado)) {

                        requiereAtencion =
                                true;

                        observacion =
                                excepcion != null
                                        ? excepcion.getMotivo()
                                        : "Turno no permitido";
                    }

                } else {

                    origen =
                            "SIN_TURNO_COMPATIBLE";

                    requiereAtencion =
                            true;

                    observacion =
                            excepcion != null
                                    ? excepcion.getMotivo()
                                    : "No se pudo asignar un turno compatible";
                }

                dias.add(
                        new ProgramacionDiaPropuesta(
                                fecha,
                                estado,
                                estadoCiclo,
                                origen,
                                requiereAtencion,
                                observacion
                        )
                );
            }

            resultado.add(
                    new ProgramacionAgentePropuesta(
                            agente.getId(),
                            agente.getCodigo(),
                            agente.getNombreCompleto(),
                            secuencia.getGrupo(),
                            secuencia.getOrden(),
                            partTime,
                            dias
                    )
            );
        }

        return resultado;
    }

    // ============================================================
    // HISTÓRICO
    // ============================================================

    private Map<Long, Map<LocalDate, EstadoProgramacion>>
    construirHistorico(
            List<ProgramacionTurno> programaciones
    ) {

        Map<Long, Map<LocalDate, EstadoProgramacion>> resultado =
                new HashMap<>();

        for (ProgramacionTurno programacion : programaciones) {

            resultado
                    .computeIfAbsent(
                            programacion
                                    .getTrabajador()
                                    .getId(),
                            ignored -> new HashMap<>()
                    )
                    .put(
                            programacion.getFecha(),
                            programacion.getEstado()
                    );
        }

        return resultado;
    }

    // ============================================================
    // EXCEPCIONES
    // ============================================================

    private boolean permite(
            AgenteProgramacionExcepcion excepcion,
            EstadoProgramacion estado
    ) {

        if (excepcion == null) {
            return true;
        }

        return switch (estado) {

            case A ->
                    Boolean.TRUE.equals(
                            excepcion.getPermiteA()
                    );

            case B ->
                    Boolean.TRUE.equals(
                            excepcion.getPermiteB()
                    );

            case C ->
                    Boolean.TRUE.equals(
                            excepcion.getPermiteC()
                    );

            default ->
                    true;
        };
    }

    // ============================================================
    // VALIDACIÓN
    // ============================================================

    private void validarRequest(
            GenerarProgramacionRequest request
    ) {

        if (request == null) {

            throw bad(
                    "La solicitud es obligatoria."
            );
        }

        if (request.plazaId() == null) {

            throw bad(
                    "La plaza es obligatoria."
            );
        }

        if (request.coberturaNormal() == null) {

            throw bad(
                    "La cobertura normal es obligatoria."
            );
        }

        YearMonth yearMonth;

        try {

            yearMonth =
                    YearMonth.of(
                            request.anio(),
                            request.mes()
                    );

        } catch (Exception ex) {

            throw bad(
                    "Año o mes inválido."
            );
        }

        if (request.diasEspeciales() != null) {

            Set<LocalDate> fechas =
                    new HashSet<>();

            for (
                    DiaEspecialRequest dia :
                    request.diasEspeciales()
            ) {

                if (dia.fecha() == null) {

                    throw bad(
                            "La fecha del día especial es obligatoria."
                    );
                }

                if (
                        !YearMonth
                                .from(dia.fecha())
                                .equals(yearMonth)
                ) {

                    throw bad(
                            "El día especial "
                                    + dia.fecha()
                                    + " no pertenece al mes solicitado."
                    );
                }

                if (!fechas.add(dia.fecha())) {

                    throw bad(
                            "Existe más de una configuración especial para "
                                    + dia.fecha() + "."
                    );
                }
            }
        }

        if (request.novedades() != null) {

            for (
                    NovedadProgramacionRequest novedad :
                    request.novedades()
            ) {

                if (novedad.trabajadorId() == null) {

                    throw bad(
                            "El trabajador de la novedad es obligatorio."
                    );
                }

                if (
                        novedad.desde() == null
                                || novedad.hasta() == null
                ) {

                    throw bad(
                            "Las fechas de la novedad son obligatorias."
                    );
                }

                if (novedad.estado() == null) {

                    throw bad(
                            "El estado de la novedad es obligatorio."
                    );
                }
            }
        }
    }

    // ============================================================
    // UTILIDADES
    // ============================================================

    private boolean esNovedad(
            EstadoProgramacion estado
    ) {

        return estado == EstadoProgramacion.V
                || estado == EstadoProgramacion.COM
                || estado == EstadoProgramacion.DM
                || estado == EstadoProgramacion.LIC;
    }

    private String claveSemana(
            LocalDate fecha
    ) {

        WeekFields weekFields =
                WeekFields.ISO;

        int year =
                fecha.get(
                        weekFields.weekBasedYear()
                );

        int week =
                fecha.get(
                        weekFields.weekOfWeekBasedYear()
                );

        return year + "-" + week;
    }

    // ============================================================
    // CONFLICTOS
    // ============================================================

    private ConflictoProgramacionResponse warning(
            String tipo,
            Trabajador trabajador,
            LocalDate fecha,
            String mensaje
    ) {

        return conflicto(
                tipo,
                "WARNING",
                trabajador,
                fecha,
                mensaje
        );
    }

    private ConflictoProgramacionResponse error(
            String tipo,
            Trabajador trabajador,
            LocalDate fecha,
            String mensaje
    ) {

        return conflicto(
                tipo,
                "ERROR",
                trabajador,
                fecha,
                mensaje
        );
    }

    private ConflictoProgramacionResponse conflicto(
            String tipo,
            String nivel,
            Trabajador trabajador,
            LocalDate fecha,
            String mensaje
    ) {

        return new ConflictoProgramacionResponse(
                tipo,
                nivel,

                trabajador == null
                        ? null
                        : trabajador.getId(),

                trabajador == null
                        ? null
                        : trabajador.getCodigo(),

                trabajador == null
                        ? null
                        : trabajador.getNombreCompleto(),

                fecha,
                mensaje
        );
    }

    // ============================================================
    // SEGURIDAD
    // ============================================================

    private Trabajador requireSupervisor() {

        Trabajador trabajador =
                currentUser.requireCurrent();

        if (
                trabajador.getRolSistema()
                        != RolSistema.SUPERVISOR
        ) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo el Supervisor puede generar una propuesta de programación"
            );
        }

        return trabajador;
    }

    private ResponseStatusException bad(
            String mensaje
    ) {

        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                mensaje
        );
    }
}


