package com.sigo.programacion.application.service;

import com.sigo.personal.infrastructure.persistence.entity.*;
import com.sigo.personal.infrastructure.persistence.repository.*;
import com.sigo.programacion.infrastructure.persistence.entity.*;
import com.sigo.programacion.infrastructure.persistence.repository.*;
import com.sigo.security.application.service.CurrentUserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ProgramacionService {

    private final ProgramacionTurnoRepository programacionRepo;

    private final ProgramacionUbicacionRepository ubicacionRepo;

    private final DistribucionPersonalRepository distribucionRepo;

    private final AgenteControladorLiderRepository liderRepo;

    private final ProgramacionSecuenciaAgenteRepository secuenciaRepo;

    private final TrabajadorRepository trabajadorRepo;

    private final PlazaRepository plazaRepo;

    private final CurrentUserService currentUser;


    /*
     * ============================================================
     * DTO - PROGRAMACIÓN DE TURNOS
     * ============================================================
     */

    public record TurnoItemRequest(

            @NotNull
            Long trabajadorId,

            @NotNull
            LocalDate fecha,

            @NotNull
            EstadoProgramacion estado

    ) {
    }


    public record GuardarProgramacionRequest(

            @NotNull
            Long plazaId,

            @NotEmpty
            List<@Valid TurnoItemRequest> programaciones

    ) {
    }


    public record ProgramacionDiaResponse(

            Long programacionId,

            Long trabajadorId,

            Integer codigoTrabajador,

            String nombreTrabajador,

            Long plazaId,

            String plazaCodigo,

            LocalDate fecha,

            EstadoProgramacion estado

    ) {
    }


    /*
     * ============================================================
     * DTO - UBICACIONES
     * ============================================================
     */

    public record UbicacionResponse(

            Long id,

            Long plazaId,

            String codigo,

            String nombre,

            TipoUbicacion tipo,

            Long viaId,

            Boolean activo,

            Integer orden

    ) {
    }


    public record GuardarUbicacionRequest(

            @NotNull
            Long plazaId,

            @NotBlank
            String codigo,

            @NotBlank
            String nombre,

            @NotNull
            TipoUbicacion tipo,

            Integer orden

    ) {
    }


    /*
     * ============================================================
     * DTO - DISTRIBUCIÓN
     * ============================================================
     */

    public record DistribucionItemRequest(

            @NotNull
            Long programacionTurnoId,

            @NotNull
            Long ubicacionId,

            String observacion

    ) {
    }


    public record GuardarDistribucionRequest(

            @NotNull
            Long plazaId,

            @NotEmpty
            List<@Valid DistribucionItemRequest> distribuciones

    ) {
    }


    public record DistribucionDiaResponse(

            Long distribucionId,

            Long programacionTurnoId,

            Long trabajadorId,

            Integer codigoTrabajador,

            String nombreTrabajador,

            LocalDate fecha,

            EstadoProgramacion estado,

            Long ubicacionId,

            String ubicacionCodigo,

            String ubicacionNombre,

            TipoUbicacion ubicacionTipo,

            String observacion

    ) {
    }


    /*
     * ============================================================
     * DTO - MI HORARIO
     * ============================================================
     */

    public record HorarioDiaResponse(

            LocalDate fecha,

            EstadoProgramacion estado,

            String ubicacionCodigo,

            String ubicacionNombre

    ) {
    }


    public record MiHorarioResponse(

            Long trabajadorId,

            Integer codigo,

            String nombre,

            Long plazaId,

            String plazaCodigo,

            String lider,

            List<HorarioDiaResponse> dias

    ) {
    }


    /*
     * ============================================================
     * DTO - LÍDERES
     * ============================================================
     */

    public record GrupoLiderRequest(

            @NotNull
            Long agenteId,

            @NotNull
            Long controladorId,

            @NotNull
            Long plazaId,

            LocalDate fechaInicio

    ) {
    }


    public record GrupoLiderResponse(

            Long id,

            Long agenteId,

            Integer agenteCodigo,

            String agenteNombre,

            Long controladorId,

            Integer controladorCodigo,

            String controladorNombre,

            Long plazaId,

            String plazaCodigo,

            LocalDate fechaInicio,

            LocalDate fechaFin,

            Boolean activo

    ) {
    }


    /*
     * ============================================================
     * DTO - RESUMEN Y COBERTURA
     * ============================================================
     */

    public record ResumenUbicacionResponse(

            String codigo,

            String nombre,

            long veces

    ) {
    }


    public record ResumenTrabajadorResponse(

            Long trabajadorId,

            Integer codigo,

            String nombre,

            List<ResumenUbicacionResponse> ubicaciones

    ) {
    }


    public record CoberturaUbicacionResponse(

            Long ubicacionId,

            String codigo,

            String nombre,

            Map<LocalDate, Long> porDia

    ) {
    }


    /*
     * ============================================================
     * DTO - SECUENCIAS
     * ============================================================
     */

    public record SecuenciaAgenteResponse(

            Long id,

            Long agenteId,

            Integer codigo,

            String nombre,

            Long plazaId,

            String plazaCodigo,

            GrupoProgramacion grupo,

            Integer orden

    ) {
    }


    public record AsignarSecuenciaRequest(

            @NotNull
            Long agenteId,

            @NotNull
            Long plazaId,

            @NotNull
            GrupoProgramacion grupo

    ) {
    }


    public record OrdenAgenteRequest(

            @NotNull
            Long agenteId,

            @NotNull
            Integer orden

    ) {
    }


    public record GuardarOrdenSecuenciaRequest(

            @NotNull
            Long plazaId,

            @NotNull
            GrupoProgramacion grupo,

            @NotEmpty
            List<@Valid OrdenAgenteRequest> agentes

    ) {
    }


    /*
     * ============================================================
     * PROGRAMACIÓN DE TURNOS
     * ============================================================
     */

    @Transactional(readOnly = true)
    public List<ProgramacionDiaResponse> listarTurnos(
            Long plazaId,
            int anio,
            int mes
    ) {

        validarAccesoLecturaPlaza(plazaId);

        YearMonth ym =
                yearMonth(
                        anio,
                        mes
                );

        return programacionRepo
                .findMes(
                        plazaId,
                        ym.atDay(1),
                        ym.atEndOfMonth()
                )
                .stream()
                .map(this::toTurno)
                .toList();
    }


    @Transactional
    public List<ProgramacionDiaResponse> guardarTurnos(
            GuardarProgramacionRequest req
    ) {

        Trabajador actual = requireSupervisor();

        Plaza plaza = plazaRepo
                .findById(req.plazaId())
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .orElseThrow(() -> bad("Plaza no válida"));

        /*
         * Cargamos los agentes una sola vez y conservamos la entidad.
         * Antes se hacía un findById() adicional por cada celda modificada.
         */
        Map<Long, Trabajador> agentesPorId = trabajadorRepo
                .findAgentesByPlaza(plaza.getId())
                .stream()
                .collect(Collectors.toMap(
                        Trabajador::getId,
                        trabajador -> trabajador
                ));

        Set<Long> trabajadorIds = new HashSet<>();
        LocalDate fechaMin = null;
        LocalDate fechaMax = null;

        for (TurnoItemRequest item : req.programaciones()) {
            if (!agentesPorId.containsKey(item.trabajadorId())) {
                throw bad(
                        "El trabajador "
                                + item.trabajadorId()
                                + " no es un agente activo de la plaza"
                );
            }

            trabajadorIds.add(item.trabajadorId());

            if (fechaMin == null || item.fecha().isBefore(fechaMin)) {
                fechaMin = item.fecha();
            }
            if (fechaMax == null || item.fecha().isAfter(fechaMax)) {
                fechaMax = item.fecha();
            }
        }

        /*
         * Una sola consulta trae las programaciones existentes involucradas
         * en el lote. Luego todo el cruce trabajador/fecha se hace en memoria.
         */
        List<ProgramacionTurno> existentes = programacionRepo
                .findParaGuardadoMasivo(
                        plaza.getId(),
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
                new ArrayList<>(req.programaciones().size());

        for (TurnoItemRequest item : req.programaciones()) {
            Trabajador trabajador = agentesPorId.get(item.trabajadorId());

            Map<LocalDate, ProgramacionTurno> porFecha =
                    existentesPorTrabajador.computeIfAbsent(
                            item.trabajadorId(),
                            ignored -> new HashMap<>()
                    );

            ProgramacionTurno programacion = porFecha.get(item.fecha());

            if (programacion == null) {
                programacion = new ProgramacionTurno();
                programacion.setTrabajador(trabajador);
                programacion.setPlaza(plaza);
                programacion.setFecha(item.fecha());
                programacion.setCreadoPor(actual);

                // Evita crear dos entidades si el mismo trabajador/fecha
                // aparece repetido dentro de la misma petición.
                porFecha.put(item.fecha(), programacion);
                paraGuardar.add(programacion);
            } else if (!paraGuardar.contains(programacion)) {
                paraGuardar.add(programacion);
            }

            programacion.setPlaza(plaza);
            programacion.setEstado(item.estado());
            programacion.setActualizadoPor(actual);
        }

        /*
         * Persistimos todo el lote de una vez. saveAllAndFlush mantiene los IDs
         * disponibles para construir la misma respuesta que ya usa el frontend.
         */
        List<ProgramacionTurno> guardados =
                programacionRepo.saveAllAndFlush(paraGuardar);

        return guardados
                .stream()
                .map(this::toTurno)
                .toList();
    }


    /*
     * ============================================================
     * UBICACIONES
     * ============================================================
     */

    @Transactional(readOnly = true)
    public List<UbicacionResponse> ubicaciones(
            Long plazaId
    ) {

        validarAccesoGestionPlaza(
                plazaId
        );


        return ubicacionRepo
                .findByPlazaIdAndActivoTrueOrderByOrdenAscCodigoAsc(
                        plazaId
                )
                .stream()
                .map(
                        this::toUbicacion
                )
                .toList();
    }


    /**
     * Lista completa para el modal de configuración.
     * Incluye ubicaciones activas e inactivas y está restringida a Supervisor.
     */
    @Transactional(readOnly = true)
    public List<UbicacionResponse> ubicacionesConfiguracion(
            Long plazaId
    ) {

        requireSupervisor();

        plazaRepo
                .findById(plazaId)
                .orElseThrow(
                        () -> notFound(
                                "Plaza no encontrada"
                        )
                );

        return ubicacionRepo
                .findByPlazaIdOrderByOrdenAscCodigoAsc(
                        plazaId
                )
                .stream()
                .map(
                        this::toUbicacion
                )
                .toList();
    }


    /**
     * Crea una nueva caseta/ubicación para una plaza.
     * No requiere cambios de esquema: usa programacion_ubicacion existente.
     */
    @Transactional
    public UbicacionResponse crearUbicacion(
            GuardarUbicacionRequest req
    ) {

        requireSupervisor();

        Plaza plaza =
                plazaRepo
                        .findById(
                                req.plazaId()
                        )
                        .orElseThrow(
                                () -> notFound(
                                        "Plaza no encontrada"
                                )
                        );

        String codigo = normalizarCodigoUbicacion(
                req.codigo()
        );

        String nombre = normalizarNombreUbicacion(
                req.nombre()
        );

        if (
                ubicacionRepo
                        .existsByPlazaIdAndCodigoIgnoreCase(
                                plaza.getId(),
                                codigo
                        )
        ) {
            throw bad(
                    "Ya existe una ubicación con el código " + codigo + " en esta plaza"
            );
        }

        int orden =
                req.orden() != null && req.orden() > 0
                        ? req.orden()
                        : siguienteOrdenUbicacion(
                        plaza.getId()
                );

        ProgramacionUbicacion ubicacion =
                new ProgramacionUbicacion();

        ubicacion.setPlaza(
                plaza
        );

        ubicacion.setCodigo(
                codigo
        );

        ubicacion.setNombre(
                nombre
        );

        ubicacion.setTipo(
                req.tipo()
        );

        ubicacion.setVia(
                null
        );

        ubicacion.setActivo(
                true
        );

        ubicacion.setOrden(
                orden
        );

        return toUbicacion(
                ubicacionRepo.saveAndFlush(
                        ubicacion
                )
        );
    }


    /**
     * Permite corregir código, nombre, tipo u orden sin alterar el historial.
     */
    @Transactional
    public UbicacionResponse actualizarUbicacion(
            Long ubicacionId,
            GuardarUbicacionRequest req
    ) {

        requireSupervisor();

        ProgramacionUbicacion ubicacion =
                ubicacionRepo
                        .findById(
                                ubicacionId
                        )
                        .orElseThrow(
                                () -> notFound(
                                        "Ubicación no encontrada"
                                )
                        );

        if (
                !Objects.equals(
                        ubicacion.getPlaza().getId(),
                        req.plazaId()
                )
        ) {
            throw bad(
                    "La ubicación no pertenece a la plaza seleccionada"
            );
        }

        String codigo = normalizarCodigoUbicacion(
                req.codigo()
        );

        String nombre = normalizarNombreUbicacion(
                req.nombre()
        );

        if (
                ubicacionRepo
                        .existsByPlazaIdAndCodigoIgnoreCaseAndIdNot(
                                req.plazaId(),
                                codigo,
                                ubicacionId
                        )
        ) {
            throw bad(
                    "Ya existe otra ubicación con el código " + codigo + " en esta plaza"
            );
        }

        ubicacion.setCodigo(
                codigo
        );

        ubicacion.setNombre(
                nombre
        );

        ubicacion.setTipo(
                req.tipo()
        );

        if (
                req.tipo() != TipoUbicacion.VIA
        ) {
            ubicacion.setVia(
                    null
            );
        }

        if (
                req.orden() != null && req.orden() > 0
        ) {
            ubicacion.setOrden(
                    req.orden()
            );
        }

        return toUbicacion(
                ubicacionRepo.saveAndFlush(
                        ubicacion
                )
        );
    }


    /**
     * "Quitar" una caseta significa desactivarla.
     * No se elimina físicamente para conservar distribuciones históricas.
     */
    @Transactional
    public UbicacionResponse cambiarEstadoUbicacion(
            Long ubicacionId,
            boolean activo
    ) {

        requireSupervisor();

        ProgramacionUbicacion ubicacion =
                ubicacionRepo
                        .findById(
                                ubicacionId
                        )
                        .orElseThrow(
                                () -> notFound(
                                        "Ubicación no encontrada"
                                )
                        );

        ubicacion.setActivo(
                activo
        );

        return toUbicacion(
                ubicacionRepo.saveAndFlush(
                        ubicacion
                )
        );
    }


    /*
     * ============================================================
     * DISTRIBUCIÓN
     * ============================================================
     */

    @Transactional(readOnly = true)
    public List<DistribucionDiaResponse> listarDistribucion(
            Long plazaId,
            int anio,
            int mes
    ) {

        validarAccesoGestionPlaza(
                plazaId
        );


        YearMonth ym =
                yearMonth(
                        anio,
                        mes
                );


        return distribucionRepo
                .findMes(
                        plazaId,
                        ym.atDay(1),
                        ym.atEndOfMonth()
                )
                .stream()
                .map(
                        this::toDistribucion
                )
                .toList();
    }


    @Transactional
    public List<DistribucionDiaResponse> guardarDistribucion(
            GuardarDistribucionRequest req
    ) {

        Trabajador actual =
                validarAccesoGestionPlaza(
                        req.plazaId()
                );


        List<DistribucionPersonal> guardados =
                new ArrayList<>();


        for (
                DistribucionItemRequest item :
                req.distribuciones()
        ) {

            ProgramacionTurno programacion =
                    programacionRepo
                            .findById(
                                    item.programacionTurnoId()
                            )
                            .orElseThrow(
                                    () ->
                                            bad(
                                                    "Programación no encontrada: "
                                                            + item.programacionTurnoId()
                                            )
                            );


            if (
                    !Objects.equals(
                            programacion
                                    .getPlaza()
                                    .getId(),

                            req.plazaId()
                    )
            ) {

                throw bad(
                        "La programación no pertenece a la plaza"
                );
            }


            if (
                    !programacion
                            .getEstado()
                            .esOperativo()
            ) {

                throw bad(
                        "Solo los estados A, B o C pueden tener caseta"
                );
            }


            ProgramacionUbicacion ubicacion =
                    ubicacionRepo
                            .findById(
                                    item.ubicacionId()
                            )
                            .filter(
                                    x ->
                                            Boolean.TRUE.equals(
                                                    x.getActivo()
                                            )
                            )
                            .orElseThrow(
                                    () ->
                                            bad(
                                                    "Ubicación no válida"
                                            )
                            );


            if (
                    !Objects.equals(
                            ubicacion
                                    .getPlaza()
                                    .getId(),

                            req.plazaId()
                    )
            ) {

                throw bad(
                        "La ubicación no pertenece a la plaza"
                );
            }


            DistribucionPersonal distribucion =
                    distribucionRepo
                            .findByProgramacionTurnoId(
                                    programacion.getId()
                            )
                            .orElseGet(
                                    () -> {

                                        DistribucionPersonal nueva =
                                                new DistribucionPersonal();

                                        nueva.setProgramacionTurno(
                                                programacion
                                        );

                                        nueva.setAsignadoPor(
                                                actual
                                        );

                                        return nueva;
                                    }
                            );


            distribucion.setUbicacion(
                    ubicacion
            );

            distribucion.setObservacion(
                    item.observacion()
            );

            distribucion.setActualizadoPor(
                    actual
            );


            guardados.add(
                    distribucionRepo.save(
                            distribucion
                    )
            );
        }


        distribucionRepo.flush();


        return guardados
                .stream()
                .map(
                        this::toDistribucion
                )
                .toList();
    }


    /*
     * ============================================================
     * RESUMEN
     * ============================================================
     */

    @Transactional(readOnly = true)
    public ResumenTrabajadorResponse resumen(
            Long trabajadorId,
            int anio,
            int mes
    ) {

        Trabajador trabajador =
                trabajadorRepo
                        .findById(
                                trabajadorId
                        )
                        .orElseThrow(
                                () ->
                                        notFound(
                                                "Trabajador no encontrado"
                                        )
                        );


        if (
                trabajador.getPlaza() == null
        ) {

            throw bad(
                    "El trabajador no tiene plaza"
            );
        }


        validarAccesoGestionPlaza(
                trabajador
                        .getPlaza()
                        .getId()
        );


        YearMonth ym =
                yearMonth(
                        anio,
                        mes
                );


        List<DistribucionPersonal> datos =
                distribucionRepo
                        .findByTrabajadorMes(
                                trabajador.getId(),
                                ym.atDay(1),
                                ym.atEndOfMonth()
                        );


        Map<Long, Long> count =
                new LinkedHashMap<>();


        Map<Long, ProgramacionUbicacion> refs =
                new LinkedHashMap<>();


        for (
                DistribucionPersonal d :
                datos
        ) {

            Long id =
                    d.getUbicacion()
                            .getId();


            refs.putIfAbsent(
                    id,
                    d.getUbicacion()
            );


            count.merge(
                    id,
                    1L,
                    Long::sum
            );
        }


        List<ResumenUbicacionResponse> ubicaciones =
                refs
                        .entrySet()
                        .stream()
                        .map(
                                entry ->
                                        new ResumenUbicacionResponse(

                                                entry
                                                        .getValue()
                                                        .getCodigo(),

                                                entry
                                                        .getValue()
                                                        .getNombre(),

                                                count.getOrDefault(
                                                        entry.getKey(),
                                                        0L
                                                )
                                        )
                        )
                        .toList();


        return new ResumenTrabajadorResponse(

                trabajador.getId(),

                trabajador.getCodigo(),

                trabajador.getNombreCompleto(),

                ubicaciones
        );
    }


    /*
     * ============================================================
     * COBERTURA
     * ============================================================
     */

    @Transactional(readOnly = true)
    public List<CoberturaUbicacionResponse> cobertura(
            Long plazaId,
            int anio,
            int mes
    ) {

        validarAccesoGestionPlaza(
                plazaId
        );


        YearMonth ym =
                yearMonth(
                        anio,
                        mes
                );


        List<DistribucionPersonal> datos =
                distribucionRepo
                        .findMes(
                                plazaId,
                                ym.atDay(1),
                                ym.atEndOfMonth()
                        );


        Map<Long, Map<LocalDate, Long>> conteos =
                new HashMap<>();


        for (
                DistribucionPersonal d :
                datos
        ) {

            conteos
                    .computeIfAbsent(
                            d.getUbicacion()
                                    .getId(),

                            key ->
                                    new TreeMap<>()
                    )
                    .merge(
                            d.getProgramacionTurno()
                                    .getFecha(),

                            1L,

                            Long::sum
                    );
        }


        return ubicacionRepo
                .findByPlazaIdOrderByOrdenAscCodigoAsc(
                        plazaId
                )
                .stream()
                .filter(
                        ubicacion ->
                                Boolean.TRUE.equals(
                                        ubicacion.getActivo()
                                )
                                        ||
                                        conteos.containsKey(
                                                ubicacion.getId()
                                        )
                )
                .map(
                        ubicacion ->
                                new CoberturaUbicacionResponse(

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


    /*
     * ============================================================
     * MI HORARIO
     * ============================================================
     */

    @Transactional(readOnly = true)
    public MiHorarioResponse miHorario(
            LocalDate desde,
            LocalDate hasta
    ) {

        Trabajador actual =
                currentUser.requireCurrent();


        if (
                desde == null
                        ||
                        hasta == null
                        ||
                        hasta.isBefore(
                                desde
                        )
        ) {

            throw bad(
                    "Rango de fechas inválido"
            );
        }


        if (
                ChronoUnit.DAYS.between(
                        desde,
                        hasta
                ) > 31
        ) {

            throw bad(
                    "El rango máximo permitido es 32 días"
            );
        }


        Map<LocalDate, ProgramacionTurno> turnos =
                new HashMap<>();


        programacionRepo
                .findHorario(
                        actual.getId(),
                        desde,
                        hasta
                )
                .forEach(
                        programacion ->
                                turnos.put(
                                        programacion.getFecha(),
                                        programacion
                                )
                );


        Map<Long, DistribucionPersonal> distribuciones =
                new HashMap<>();


        distribucionRepo
                .findByTrabajadorMes(
                        actual.getId(),
                        desde,
                        hasta
                )
                .forEach(
                        distribucion ->
                                distribuciones.put(
                                        distribucion
                                                .getProgramacionTurno()
                                                .getId(),

                                        distribucion
                                )
                );


        List<HorarioDiaResponse> dias =
                new ArrayList<>();


        for (
                LocalDate fecha = desde;

                !fecha.isAfter(
                        hasta
                );

                fecha = fecha.plusDays(
                        1
                )
        ) {

            ProgramacionTurno programacion =
                    turnos.get(
                            fecha
                    );


            if (
                    programacion == null
            ) {

                dias.add(
                        new HorarioDiaResponse(
                                fecha,
                                null,
                                null,
                                null
                        )
                );

                continue;
            }


            DistribucionPersonal distribucion =
                    distribuciones.get(
                            programacion.getId()
                    );


            dias.add(
                    new HorarioDiaResponse(

                            fecha,

                            programacion.getEstado(),

                            distribucion == null
                                    ? null
                                    : distribucion
                                    .getUbicacion()
                                    .getCodigo(),

                            distribucion == null
                                    ? null
                                    : distribucion
                                    .getUbicacion()
                                    .getNombre()
                    )
            );
        }


        String lider =
                liderRepo
                        .findByAgenteIdAndActivoTrue(
                                actual.getId()
                        )
                        .map(
                                x ->
                                        x.getControlador()
                                                .getNombreCompleto()
                        )
                        .orElse(
                                null
                        );


        return new MiHorarioResponse(

                actual.getId(),

                actual.getCodigo(),

                actual.getNombreCompleto(),

                actual.getPlaza() == null
                        ? null
                        : actual
                        .getPlaza()
                        .getId(),

                actual.getPlaza() == null
                        ? null
                        : actual
                        .getPlaza()
                        .getCodigo(),

                lider,

                dias
        );
    }


    /*
     * ============================================================
     * LÍDERES
     * ============================================================
     */

    @Transactional(readOnly = true)
    public List<GrupoLiderResponse> listarLideres(
            Long plazaId
    ) {

        requireSupervisor();

        return liderRepo
                .findLideresActivosPorPlaza(plazaId)
                .stream()
                .map(this::toLider)
                .toList();
    }


    @Transactional
    public GrupoLiderResponse asignarLider(
            GrupoLiderRequest req
    ) {

        Trabajador actual =
                requireSupervisor();


        Plaza plaza =
                plazaRepo
                        .findById(
                                req.plazaId()
                        )
                        .filter(
                                p ->
                                        Boolean.TRUE.equals(
                                                p.getActivo()
                                        )
                        )
                        .orElseThrow(
                                () ->
                                        bad(
                                                "Plaza no válida"
                                        )
                        );


        Trabajador agente =
                trabajadorRepo
                        .findById(
                                req.agenteId()
                        )
                        .filter(
                                t ->
                                        Boolean.TRUE.equals(
                                                t.getActivo()
                                        )
                        )
                        .orElseThrow(
                                () ->
                                        bad(
                                                "Agente no encontrado"
                                        )
                        );


        Trabajador controlador =
                trabajadorRepo
                        .findById(
                                req.controladorId()
                        )
                        .filter(
                                t ->
                                        Boolean.TRUE.equals(
                                                t.getActivo()
                                        )
                        )
                        .orElseThrow(
                                () ->
                                        bad(
                                                "Controlador no encontrado"
                                        )
                        );


        Set<Long> agentes =
                trabajadorRepo
                        .findAgentesByPlaza(
                                plaza.getId()
                        )
                        .stream()
                        .map(
                                Trabajador::getId
                        )
                        .collect(
                                Collectors.toSet()
                        );


        if (
                !agentes.contains(
                        agente.getId()
                )
        ) {

            throw bad(
                    "El trabajador seleccionado no es un agente activo de la plaza"
            );
        }


        if (
                controlador.getRolSistema()
                        != RolSistema.CONTROLADOR
        ) {

            throw bad(
                    "El líder debe tener rol CONTROLADOR"
            );
        }


        if (
                controlador.getPlaza() == null
                        ||
                        !Objects.equals(

                                controlador
                                        .getPlaza()
                                        .getId(),

                                plaza.getId()
                        )
        ) {

            throw bad(
                    "El controlador no pertenece a la plaza"
            );
        }


        LocalDate inicio =
                req.fechaInicio() == null
                        ? LocalDate.now()
                        : req.fechaInicio();


        liderRepo
                .findByAgenteIdAndActivoTrue(
                        agente.getId()
                )
                .ifPresent(
                        anterior -> {

                            anterior.setActivo(
                                    false
                            );


                            LocalDate fin =
                                    inicio.minusDays(
                                            1
                                    );


                            if (
                                    fin.isBefore(
                                            anterior.getFechaInicio()
                                    )
                            ) {

                                fin =
                                        anterior.getFechaInicio();
                            }


                            anterior.setFechaFin(
                                    fin
                            );


                            liderRepo.save(
                                    anterior
                            );
                        }
                );


        AgenteControladorLider nuevo =
                new AgenteControladorLider();


        nuevo.setAgente(
                agente
        );

        nuevo.setControlador(
                controlador
        );

        nuevo.setPlaza(
                plaza
        );

        nuevo.setFechaInicio(
                inicio
        );

        nuevo.setActivo(
                true
        );

        nuevo.setAsignadoPor(
                actual
        );


        return toLider(
                liderRepo.save(
                        nuevo
                )
        );
    }


    /*
     * ============================================================
     * SECUENCIAS
     * ============================================================
     */

    @Transactional(readOnly = true)
    public List<SecuenciaAgenteResponse> listarSecuencias(
            Long plazaId
    ) {

        validarAccesoLecturaPlaza(
                plazaId
        );


        return secuenciaRepo
                .findActivasByPlazaId(
                        plazaId
                )
                .stream()
                .map(
                        this::toSecuencia
                )
                .toList();
    }


    /*
     * Cambia un agente a una secuencia.
     *
     * Si ya pertenecía a otra secuencia:
     * - se elimina de la posición anterior
     * - se normaliza el grupo anterior
     * - se agrega al final del grupo nuevo
     */
    @Transactional
    public SecuenciaAgenteResponse asignarSecuencia(
            AsignarSecuenciaRequest req
    ) {

        Trabajador actual =
                requireSupervisor();


        Plaza plaza =
                plazaRepo
                        .findById(
                                req.plazaId()
                        )
                        .filter(
                                p ->
                                        Boolean.TRUE.equals(
                                                p.getActivo()
                                        )
                        )
                        .orElseThrow(
                                () ->
                                        bad(
                                                "Plaza no válida"
                                        )
                        );


        Trabajador agente =
                trabajadorRepo
                        .findById(
                                req.agenteId()
                        )
                        .filter(
                                t ->
                                        Boolean.TRUE.equals(
                                                t.getActivo()
                                        )
                        )
                        .orElseThrow(
                                () ->
                                        bad(
                                                "Agente no encontrado"
                                        )
                        );


        validarAgentePertenecePlaza(
                agente,
                plaza.getId()
        );


        ProgramacionSecuenciaAgente registro =
                secuenciaRepo
                        .findByAgenteId(
                                agente.getId()
                        )
                        .orElseGet(
                                () -> {

                                    ProgramacionSecuenciaAgente nuevo =
                                            new ProgramacionSecuenciaAgente();

                                    nuevo.setAgente(
                                            agente
                                    );

                                    nuevo.setPlaza(
                                            plaza
                                    );

                                    return nuevo;
                                }
                        );


        GrupoProgramacion grupoAnterior =
                registro.getGrupo();


        /*
         * Si ya está en ese mismo grupo,
         * no hacemos ningún movimiento.
         */
        if (
                grupoAnterior ==
                        req.grupo()
        ) {

            return toSecuencia(
                    registro
            );
        }


        /*
         * Primero lo dejamos temporalmente
         * sin grupo para evitar colisiones
         * del índice único:
         *
         * plaza + grupo + orden
         */
        registro.setGrupo(
                null
        );

        registro.setOrden(
                null
        );

        registro.setActualizadoPor(
                actual
        );


        registro =
                secuenciaRepo.save(
                        registro
                );


        secuenciaRepo.flush();


        /*
         * Reordenamos el grupo anterior.
         */
        if (
                grupoAnterior != null
        ) {

            normalizarGrupo(
                    plaza.getId(),
                    grupoAnterior,
                    actual
            );
        }


        /*
         * Normalizamos también el grupo
         * de destino antes de insertar.
         */
        normalizarGrupo(
                plaza.getId(),
                req.grupo(),
                actual
        );


        List<ProgramacionSecuenciaAgente> destino =
                secuenciaRepo
                        .findByPlazaIdAndGrupoOrderByOrdenAsc(
                                plaza.getId(),
                                req.grupo()
                        );


        /*
         * El agente entra al final
         * de la nueva secuencia.
         */
        registro.setGrupo(
                req.grupo()
        );

        registro.setOrden(
                destino.size() + 1
        );

        registro.setActualizadoPor(
                actual
        );


        registro =
                secuenciaRepo.save(
                        registro
                );


        secuenciaRepo.flush();


        return toSecuencia(
                registro
        );
    }


    /*
     * Guarda el orden completo
     * de una secuencia.
     *
     * El frontend puede mandar:
     *
     * 1 Gianinna
     * 2 Carlos
     * 3 María
     *
     * y luego:
     *
     * 1 Gianinna
     * 2 María
     * 3 Carlos
     */
    @Transactional
    public void guardarOrdenSecuencia(
            GuardarOrdenSecuenciaRequest req
    ) {

        Trabajador actual =
                requireSupervisor();


        Plaza plaza =
                plazaRepo
                        .findById(
                                req.plazaId()
                        )
                        .filter(
                                p ->
                                        Boolean.TRUE.equals(
                                                p.getActivo()
                                        )
                        )
                        .orElseThrow(
                                () ->
                                        bad(
                                                "Plaza no válida"
                                        )
                        );


        /*
         * Elimina configuraciones antiguas de agentes que ya están inactivos.
         * La tabla de secuencias representa la configuración vigente, no un histórico.
         * Esto evita que el trigger de PostgreSQL intente validar agentes inactivos
         * durante un reordenamiento y evita que posiciones antiguas ocupen el índice único.
         */
        secuenciaRepo.deleteInactivasByPlazaId(
                plaza.getId()
        );

        secuenciaRepo.flush();


        List<ProgramacionSecuenciaAgente> actuales =
                secuenciaRepo
                        .findActivasByPlazaIdAndGrupo(
                                plaza.getId(),
                                req.grupo()
                        );


        if (
                actuales.size()
                        != req.agentes().size()
        ) {

            throw bad(
                    "Debes enviar todos los integrantes de la secuencia"
            );
        }


        Set<Long> actualesIds =
                actuales
                        .stream()
                        .map(
                                x ->
                                        x.getAgente()
                                                .getId()
                        )
                        .collect(
                                Collectors.toSet()
                        );


        Set<Long> enviadosIds =
                req.agentes()
                        .stream()
                        .map(
                                OrdenAgenteRequest::agenteId
                        )
                        .collect(
                                Collectors.toSet()
                        );


        if (
                enviadosIds.size()
                        != req.agentes().size()
        ) {

            throw bad(
                    "Hay agentes repetidos en el orden enviado"
            );
        }


        if (
                !actualesIds.equals(
                        enviadosIds
                )
        ) {

            throw bad(
                    "Los agentes enviados no coinciden con los integrantes de la secuencia"
            );
        }


        /*
         * Verificamos que los órdenes
         * sean exactamente:
         *
         * 1, 2, 3 ... N
         */
        Set<Integer> ordenes =
                req.agentes()
                        .stream()
                        .map(
                                OrdenAgenteRequest::orden
                        )
                        .collect(
                                Collectors.toSet()
                        );


        if (
                ordenes.size()
                        != req.agentes().size()
        ) {

            throw bad(
                    "No pueden existir posiciones repetidas"
            );
        }


        for (
                int i = 1;
                i <= req.agentes().size();
                i++
        ) {

            if (
                    !ordenes.contains(
                            i
                    )
            ) {

                throw bad(
                        "El orden debe ser consecutivo desde 1"
                );
            }
        }


        Map<Long, ProgramacionSecuenciaAgente> porAgente =
                actuales
                        .stream()
                        .collect(
                                Collectors.toMap(

                                        x ->
                                                x.getAgente()
                                                        .getId(),

                                        x ->
                                                x
                                )
                        );


        /*
         * Muy importante:
         *
         * Primero ponemos temporalmente
         * grupo y orden en null.
         *
         * Esto evita errores por el índice:
         *
         * unique(plaza_id, grupo, orden)
         */
        for (
                ProgramacionSecuenciaAgente registro :
                actuales
        ) {

            registro.setGrupo(
                    null
            );

            registro.setOrden(
                    null
            );

            registro.setActualizadoPor(
                    actual
            );


            secuenciaRepo.save(
                    registro
            );
        }


        secuenciaRepo.flush();


        /*
         * Ordenamos lo recibido por
         * posición y restauramos el grupo.
         */
        List<OrdenAgenteRequest> ordenFinal =
                req.agentes()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        OrdenAgenteRequest::orden
                                )
                        )
                        .toList();


        for (
                OrdenAgenteRequest item :
                ordenFinal
        ) {

            ProgramacionSecuenciaAgente registro =
                    porAgente.get(
                            item.agenteId()
                    );


            registro.setGrupo(
                    req.grupo()
            );

            registro.setOrden(
                    item.orden()
            );

            registro.setActualizadoPor(
                    actual
            );


            secuenciaRepo.save(
                    registro
            );
        }


        secuenciaRepo.flush();
    }


    /*
     * ============================================================
     * HELPERS SECUENCIAS
     * ============================================================
     */

    private void normalizarGrupo(
            Long plazaId,
            GrupoProgramacion grupo,
            Trabajador actual
    ) {

        secuenciaRepo.deleteInactivasByPlazaId(
                plazaId
        );

        secuenciaRepo.flush();


        List<ProgramacionSecuenciaAgente> registros =
                secuenciaRepo
                        .findActivasByPlazaIdAndGrupo(
                                plazaId,
                                grupo
                        );


        if (
                registros.isEmpty()
        ) {
            return;
        }


        /*
         * Quitamos temporalmente
         * grupo y orden para no chocar
         * con el índice único.
         */
        for (
                ProgramacionSecuenciaAgente registro :
                registros
        ) {

            registro.setGrupo(
                    null
            );

            registro.setOrden(
                    null
            );

            registro.setActualizadoPor(
                    actual
            );


            secuenciaRepo.save(
                    registro
            );
        }


        secuenciaRepo.flush();


        int orden =
                1;


        for (
                ProgramacionSecuenciaAgente registro :
                registros
        ) {

            registro.setGrupo(
                    grupo
            );

            registro.setOrden(
                    orden++
            );

            registro.setActualizadoPor(
                    actual
            );


            secuenciaRepo.save(
                    registro
            );
        }


        secuenciaRepo.flush();
    }


    private void validarAgentePertenecePlaza(
            Trabajador agente,
            Long plazaId
    ) {

        if (
                agente.getPlaza() == null
        ) {

            throw bad(
                    "El agente no tiene plaza asignada"
            );
        }


        if (
                !Objects.equals(
                        agente
                                .getPlaza()
                                .getId(),

                        plazaId
                )
        ) {

            throw bad(
                    "El agente no pertenece a la plaza seleccionada"
            );
        }


        Set<Long> agentesValidos =
                trabajadorRepo
                        .findAgentesByPlaza(
                                plazaId
                        )
                        .stream()
                        .map(
                                Trabajador::getId
                        )
                        .collect(
                                Collectors.toSet()
                        );


        if (
                !agentesValidos.contains(
                        agente.getId()
                )
        ) {

            throw bad(
                    "El trabajador seleccionado no es un agente activo de la plaza"
            );
        }
    }


    /*
     * ============================================================
     * SEGURIDAD
     * ============================================================
     */

    private Trabajador validarAccesoLecturaPlaza(
            Long plazaId
    ) {

        Trabajador actual =
                currentUser.requireCurrent();


        if (
                actual.getRolSistema()
                        == RolSistema.SUPERVISOR
        ) {

            return actual;
        }


        if (
                actual.getRolSistema()
                        != RolSistema.CONTROLADOR
        ) {

            throw forbidden(
                    "No tienes acceso a la programación mensual"
            );
        }


        if (
                actual.getPlaza() == null
                        ||
                        !Objects.equals(

                                actual
                                        .getPlaza()
                                        .getId(),

                                plazaId
                        )
        ) {

            throw forbidden(
                    "Solo puedes consultar tu propia plaza"
            );
        }


        return actual;
    }


    private Trabajador validarAccesoGestionPlaza(
            Long plazaId
    ) {

        Trabajador actual =
                currentUser.requireCurrent();


        if (
                actual.getRolSistema()
                        == RolSistema.SUPERVISOR
        ) {

            return actual;
        }


        if (
                actual.getRolSistema()
                        != RolSistema.CONTROLADOR
        ) {

            throw forbidden(
                    "Solo Supervisor o Controlador puede gestionar la distribución"
            );
        }


        if (
                actual.getPlaza() == null
                        ||
                        !Objects.equals(

                                actual
                                        .getPlaza()
                                        .getId(),

                                plazaId
                        )
        ) {

            throw forbidden(
                    "El controlador solo puede gestionar su propia plaza"
            );
        }


        return actual;
    }


    private Trabajador requireSupervisor() {

        Trabajador trabajador =
                currentUser.requireCurrent();


        if (
                trabajador.getRolSistema()
                        != RolSistema.SUPERVISOR
        ) {

            throw forbidden(
                    "Solo el Supervisor puede realizar esta operación"
            );
        }


        return trabajador;
    }


    /*
     * ============================================================
     * MAPPERS
     * ============================================================
     */

    private ProgramacionDiaResponse toTurno(
            ProgramacionTurno programacion
    ) {

        return new ProgramacionDiaResponse(

                programacion.getId(),

                programacion
                        .getTrabajador()
                        .getId(),

                programacion
                        .getTrabajador()
                        .getCodigo(),

                programacion
                        .getTrabajador()
                        .getNombreCompleto(),

                programacion
                        .getPlaza()
                        .getId(),

                programacion
                        .getPlaza()
                        .getCodigo(),

                programacion.getFecha(),

                programacion.getEstado()
        );
    }


    private UbicacionResponse toUbicacion(
            ProgramacionUbicacion ubicacion
    ) {

        return new UbicacionResponse(

                ubicacion.getId(),

                ubicacion
                        .getPlaza()
                        .getId(),

                ubicacion.getCodigo(),

                ubicacion.getNombre(),

                ubicacion.getTipo(),

                ubicacion.getVia() == null
                        ? null
                        : ubicacion
                        .getVia()
                        .getId(),

                ubicacion.getActivo(),

                ubicacion.getOrden()
        );
    }


    private DistribucionDiaResponse toDistribucion(
            DistribucionPersonal distribucion
    ) {

        ProgramacionTurno programacion =
                distribucion
                        .getProgramacionTurno();


        ProgramacionUbicacion ubicacion =
                distribucion
                        .getUbicacion();


        return new DistribucionDiaResponse(

                distribucion.getId(),

                programacion.getId(),

                programacion
                        .getTrabajador()
                        .getId(),

                programacion
                        .getTrabajador()
                        .getCodigo(),

                programacion
                        .getTrabajador()
                        .getNombreCompleto(),

                programacion.getFecha(),

                programacion.getEstado(),

                ubicacion.getId(),

                ubicacion.getCodigo(),

                ubicacion.getNombre(),

                ubicacion.getTipo(),

                distribucion.getObservacion()
        );
    }


    private GrupoLiderResponse toLider(
            AgenteControladorLider lider
    ) {

        return new GrupoLiderResponse(

                lider.getId(),

                lider
                        .getAgente()
                        .getId(),

                lider
                        .getAgente()
                        .getCodigo(),

                lider
                        .getAgente()
                        .getNombreCompleto(),

                lider
                        .getControlador()
                        .getId(),

                lider
                        .getControlador()
                        .getCodigo(),

                lider
                        .getControlador()
                        .getNombreCompleto(),

                lider
                        .getPlaza()
                        .getId(),

                lider
                        .getPlaza()
                        .getCodigo(),

                lider.getFechaInicio(),

                lider.getFechaFin(),

                lider.getActivo()
        );
    }


    private SecuenciaAgenteResponse toSecuencia(
            ProgramacionSecuenciaAgente secuencia
    ) {

        return new SecuenciaAgenteResponse(

                secuencia.getId(),

                secuencia
                        .getAgente()
                        .getId(),

                secuencia
                        .getAgente()
                        .getCodigo(),

                secuencia
                        .getAgente()
                        .getNombreCompleto(),

                secuencia
                        .getPlaza()
                        .getId(),

                secuencia
                        .getPlaza()
                        .getCodigo(),

                secuencia.getGrupo(),

                secuencia.getOrden()
        );
    }


    /*
     * ============================================================
     * UTILIDADES
     * ============================================================
     */

    private String normalizarCodigoUbicacion(
            String codigo
    ) {

        String valor =
                codigo == null
                        ? ""
                        : codigo.trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (
                valor.isBlank()
        ) {
            throw bad(
                    "El código de la ubicación es obligatorio"
            );
        }

        if (
                valor.length() > 30
        ) {
            throw bad(
                    "El código de la ubicación no puede superar 30 caracteres"
            );
        }

        return valor;
    }


    private String normalizarNombreUbicacion(
            String nombre
    ) {

        String valor =
                nombre == null
                        ? ""
                        : nombre.trim();

        if (
                valor.isBlank()
        ) {
            throw bad(
                    "El nombre de la ubicación es obligatorio"
            );
        }

        if (
                valor.length() > 100
        ) {
            throw bad(
                    "El nombre de la ubicación no puede superar 100 caracteres"
            );
        }

        return valor;
    }


    private int siguienteOrdenUbicacion(
            Long plazaId
    ) {

        return ubicacionRepo
                .findByPlazaIdOrderByOrdenAscCodigoAsc(
                        plazaId
                )
                .stream()
                .map(
                        ProgramacionUbicacion::getOrden
                )
                .filter(
                        Objects::nonNull
                )
                .max(
                        Integer::compareTo
                )
                .orElse(
                        0
                )
                + 1;
    }


    private YearMonth yearMonth(
            int anio,
            int mes
    ) {

        try {

            return YearMonth.of(
                    anio,
                    mes
            );

        }
        catch (
                Exception e
        ) {

            throw bad(
                    "Año o mes inválido"
            );
        }
    }


    private ResponseStatusException bad(
            String mensaje
    ) {

        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                mensaje
        );
    }


    private ResponseStatusException notFound(
            String mensaje
    ) {

        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                mensaje
        );
    }


    private ResponseStatusException forbidden(
            String mensaje
    ) {

        return new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                mensaje
        );
    }
}
