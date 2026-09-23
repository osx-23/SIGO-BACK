package com.sigo.asistencia.application.service;

import com.sigo.asistencia.api.dto.*; import com.sigo.relevo.api.dto.*;
import com.sigo.personal.infrastructure.persistence.entity.*; import com.sigo.asistencia.infrastructure.persistence.entity.*; import com.sigo.relevo.infrastructure.persistence.entity.*;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ResourceNotFoundException;
import com.sigo.personal.infrastructure.persistence.repository.*; import com.sigo.asistencia.infrastructure.persistence.repository.*; import com.sigo.relevo.infrastructure.persistence.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;
    private final AsistenciaAusenciaRepository ausenciaRepository;
    private final AsistenciaEvidenciaRepository evidenciaRepository;

    private final PlazaRepository plazaRepository;
    private final TurnoRepository turnoRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final MotivoAusenciaRepository motivoRepository;

    /*
     * =========================================================
     * REGISTRAR ASISTENCIA
     * =========================================================
     */
    @Transactional
    public AsistenciaResponse registrar(AsistenciaRequest request) {

        Plaza plaza = plazaRepository
                .findById(request.plazaId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Plaza no encontrada"
                        )
                );

        Turno turno = turnoRepository
                .findById(request.turnoId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Turno no encontrado"
                        )
                );

        Trabajador controlador = trabajadorRepository
                .findById(request.controladorId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Controlador no encontrado"
                        )
                );

        validarControlador(controlador);

        /*
         * Validar que el controlador pertenezca
         * a la plaza seleccionada.
         */
        if (controlador.getPlaza() == null
                || !controlador.getPlaza().getId().equals(plaza.getId())) {

            throw new BusinessException(
                    "El controlador seleccionado no pertenece a la plaza"
            );
        }

        /*
         * Evitar duplicados por:
         * plaza + turno + fecha
         */
        if (asistenciaRepository
                .existsByPlazaIdAndTurnoIdAndFecha(
                        request.plazaId(),
                        request.turnoId(),
                        request.fecha()
                )) {

            throw new BusinessException(
                    "Ya existe una asistencia para esa plaza, turno y fecha"
            );
        }

        int programados = request.programados();
        int presentes = request.presentes();

        int apoyoSolicitado =
                request.apoyoSolicitado() == null
                        ? 0
                        : request.apoyoSolicitado();

        if (programados < 0) {
            throw new BusinessException(
                    "La cantidad de programados no puede ser negativa"
            );
        }

        if (presentes < 0 || presentes > programados) {
            throw new BusinessException(
                    "Los presentes deben estar entre 0 y "
                            + programados
            );
        }

        if (apoyoSolicitado < 0) {
            throw new BusinessException(
                    "El apoyo solicitado no puede ser negativo"
            );
        }

        List<AusenciaRequest> ausencias =
                request.ausencias() == null
                        ? List.of()
                        : request.ausencias();

        int cantidadAusentes =
                programados - presentes;

        /*
         * La cantidad de trabajadores registrados
         * como ausentes debe coincidir con el total.
         */
        if (ausencias.size() != cantidadAusentes) {
            throw new BusinessException(
                    "Debe registrar exactamente "
                            + cantidadAusentes
                            + " ausencia(s)"
            );
        }

        /*
         * Evitar que una misma persona aparezca
         * varias veces como ausente.
         */
        Set<Long> trabajadoresUnicos =
                ausencias.stream()
                        .map(AusenciaRequest::trabajadorId)
                        .collect(Collectors.toSet());

        if (trabajadoresUnicos.size()
                != ausencias.size()) {

            throw new BusinessException(
                    "No puede registrar al mismo trabajador ausente dos veces"
            );
        }

        /*
         * Guardar registro principal.
         */
        AsistenciaRegistro asistencia =
                new AsistenciaRegistro();

        asistencia.setPlaza(plaza);
        asistencia.setTurno(turno);
        asistencia.setControlador(controlador);
        asistencia.setFecha(request.fecha());
        asistencia.setProgramados(programados);
        asistencia.setPresentes(presentes);
        asistencia.setApoyoSolicitado(apoyoSolicitado);
        asistencia.setDetalleApoyo(
                apoyoSolicitado > 0
                        ? limpiarTexto(request.detalleApoyo())
                        : null
        );
        asistencia.setNotas(request.notas());

        asistencia =
                asistenciaRepository.saveAndFlush(
                        asistencia
                );

        /*
         * Guardar ausencias.
         */
        for (AusenciaRequest ausenciaRequest
                : ausencias) {

            Trabajador trabajador =
                    trabajadorRepository
                            .findById(
                                    ausenciaRequest
                                            .trabajadorId()
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Trabajador no encontrado: "
                                                    + ausenciaRequest
                                                    .trabajadorId()
                                    )
                            );

            if (!Boolean.TRUE.equals(
                    trabajador.getActivo()
            )) {
                throw new BusinessException(
                        "El trabajador "
                                + trabajador.getCodigo()
                                + " está inactivo"
                );
            }

            /*
             * Validar que el trabajador ausente
             * pertenezca a la plaza seleccionada.
             */
            if (trabajador.getPlaza() == null
                    || !trabajador.getPlaza()
                    .getId()
                    .equals(plaza.getId())) {

                throw new BusinessException(
                        "El trabajador "
                                + trabajador.getNombreCompleto()
                                + " no pertenece a la plaza seleccionada"
                );
            }

            MotivoAusencia motivo =
                    motivoRepository
                            .findById(
                                    ausenciaRequest.motivoId()
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Motivo no encontrado: "
                                                    + ausenciaRequest
                                                    .motivoId()
                                    )
                            );

            AsistenciaAusencia ausencia =
                    new AsistenciaAusencia();

            ausencia.setAsistencia(asistencia);
            ausencia.setTrabajador(trabajador);
            ausencia.setMotivo(motivo);
            ausencia.setObservacion(
                    ausenciaRequest.observacion()
            );

            ausenciaRepository.save(ausencia);
        }

        /*
         * Mantener compatibilidad con evidencias
         * que vengan directamente como URL.
         */
        if (request.evidencias() != null) {

            for (String url
                    : request.evidencias()) {

                if (url == null || url.isBlank()) {
                    continue;
                }

                AsistenciaEvidencia evidencia =
                        new AsistenciaEvidencia();

                evidencia.setAsistencia(
                        asistencia
                );

                evidencia.setUrlArchivo(url);
                evidencia.setTipo("foto");

                evidenciaRepository.save(
                        evidencia
                );
            }
        }

        return obtenerPorId(
                asistencia.getId()
        );
    }
    /*
     * =========================================================
     * ACTUALIZAR ASISTENCIA
     * =========================================================
     */
    @Transactional
    public AsistenciaResponse actualizar(
            Long id,
            AsistenciaUpdateRequest request
    ) {

        /*
         * Buscar asistencia existente.
         */
        AsistenciaRegistro asistencia =
                asistenciaRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Asistencia no encontrada"
                                )
                        );

        /*
         * Buscar plaza.
         */
        Plaza plaza =
                plazaRepository
                        .findById(request.plazaId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Plaza no encontrada"
                                )
                        );

        /*
         * Buscar turno.
         */
        Turno turno =
                turnoRepository
                        .findById(request.turnoId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Turno no encontrado"
                                )
                        );

        /*
         * Buscar controlador.
         */
        Trabajador controlador =
                trabajadorRepository
                        .findById(request.controladorId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Controlador no encontrado"
                                )
                        );

        /*
         * Validar cargo y estado del controlador.
         */
        validarControlador(controlador);

        /*
         * Validar que el controlador pertenezca
         * a la plaza seleccionada.
         */
        if (controlador.getPlaza() == null
                || !controlador
                .getPlaza()
                .getId()
                .equals(plaza.getId())) {

            throw new BusinessException(
                    "El controlador seleccionado no pertenece a la plaza"
            );
        }

        /*
         * Evitar duplicados.
         *
         * Busca otra asistencia con la misma:
         * plaza + turno + fecha
         *
         * ignorando el registro que estamos editando.
         */
        if (asistenciaRepository
                .existsByPlazaIdAndTurnoIdAndFechaAndIdNot(
                        request.plazaId(),
                        request.turnoId(),
                        request.fecha(),
                        id
                )) {

            throw new BusinessException(
                    "Ya existe otra asistencia para esa plaza, turno y fecha"
            );
        }

        int programados =
                request.programados();

        int presentes =
                request.presentes();

        int apoyoSolicitado =
                request.apoyoSolicitado() == null
                        ? 0
                        : request.apoyoSolicitado();

        /*
         * Validar programados.
         */
        if (programados < 0) {

            throw new BusinessException(
                    "La cantidad de programados no puede ser negativa"
            );
        }

        /*
         * Validar presentes.
         */
        if (presentes < 0
                || presentes > programados) {

            throw new BusinessException(
                    "Los presentes deben estar entre 0 y "
                            + programados
            );
        }

        if (apoyoSolicitado < 0) {
            throw new BusinessException(
                    "El apoyo solicitado no puede ser negativo"
            );
        }

        /*
         * Ausencias recibidas.
         */
        List<AusenciaRequest> ausencias =
                request.ausencias() == null
                        ? List.of()
                        : request.ausencias();

        int cantidadAusentes =
                programados - presentes;

        /*
         * La cantidad de ausencias registradas
         * debe coincidir con:
         *
         * programados - presentes
         */
        if (ausencias.size()
                != cantidadAusentes) {

            throw new BusinessException(
                    "Debe registrar exactamente "
                            + cantidadAusentes
                            + " ausencia(s)"
            );
        }

        /*
         * Evitar trabajadores duplicados.
         */
        Set<Long> trabajadoresUnicos =
                ausencias
                        .stream()
                        .map(
                                AusenciaRequest::trabajadorId
                        )
                        .collect(
                                Collectors.toSet()
                        );

        if (trabajadoresUnicos.size()
                != ausencias.size()) {

            throw new BusinessException(
                    "No puede registrar al mismo trabajador ausente dos veces"
            );
        }

        /*
         * =====================================================
         * VALIDAR TODAS LAS AUSENCIAS ANTES DE MODIFICAR
         * =====================================================
         *
         * Es importante validar primero para evitar modificar
         * parcialmente el registro.
         */
        for (AusenciaRequest ausenciaRequest
                : ausencias) {

            Trabajador trabajador =
                    trabajadorRepository
                            .findById(
                                    ausenciaRequest
                                            .trabajadorId()
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Trabajador no encontrado: "
                                                    + ausenciaRequest
                                                    .trabajadorId()
                                    )
                            );

            /*
             * Trabajador activo.
             */
            if (!Boolean.TRUE.equals(
                    trabajador.getActivo()
            )) {

                throw new BusinessException(
                        "El trabajador "
                                + trabajador.getCodigo()
                                + " está inactivo"
                );
            }

            /*
             * Trabajador perteneciente
             * a la plaza seleccionada.
             */
            if (trabajador.getPlaza() == null
                    || !trabajador
                    .getPlaza()
                    .getId()
                    .equals(plaza.getId())) {

                throw new BusinessException(
                        "El trabajador "
                                + trabajador.getNombreCompleto()
                                + " no pertenece a la plaza seleccionada"
                );
            }

            /*
             * Verificar que exista el motivo.
             */
            motivoRepository
                    .findById(
                            ausenciaRequest.motivoId()
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Motivo no encontrado: "
                                            + ausenciaRequest
                                            .motivoId()
                            )
                    );
        }

        /*
         * =====================================================
         * ACTUALIZAR REGISTRO PRINCIPAL
         * =====================================================
         */
        asistencia.setPlaza(plaza);
        asistencia.setTurno(turno);
        asistencia.setControlador(controlador);
        asistencia.setFecha(request.fecha());
        asistencia.setProgramados(programados);
        asistencia.setPresentes(presentes);
        asistencia.setApoyoSolicitado(apoyoSolicitado);
        asistencia.setDetalleApoyo(
                apoyoSolicitado > 0
                        ? limpiarTexto(request.detalleApoyo())
                        : null
        );
        asistencia.setNotas(request.notas());

        asistenciaRepository.saveAndFlush(
                asistencia
        );

        /*
         * =====================================================
         * REEMPLAZAR AUSENCIAS
         * =====================================================
         *
         * Eliminamos las anteriores y volvemos a crear
         * las enviadas desde el formulario de edición.
         */
        ausenciaRepository
                .deleteByAsistenciaId(id);

        ausenciaRepository.flush();

        /*
         * Crear las nuevas ausencias.
         */
        for (AusenciaRequest ausenciaRequest
                : ausencias) {

            Trabajador trabajador =
                    trabajadorRepository
                            .findById(
                                    ausenciaRequest
                                            .trabajadorId()
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Trabajador no encontrado: "
                                                    + ausenciaRequest
                                                    .trabajadorId()
                                    )
                            );

            MotivoAusencia motivo =
                    motivoRepository
                            .findById(
                                    ausenciaRequest
                                            .motivoId()
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Motivo no encontrado: "
                                                    + ausenciaRequest
                                                    .motivoId()
                                    )
                            );

            AsistenciaAusencia ausencia =
                    new AsistenciaAusencia();

            ausencia.setAsistencia(
                    asistencia
            );

            ausencia.setTrabajador(
                    trabajador
            );

            ausencia.setMotivo(
                    motivo
            );

            ausencia.setObservacion(
                    ausenciaRequest
                            .observacion()
            );

            ausenciaRepository.save(
                    ausencia
            );
        }

        /*
         * Asegurar que las nuevas ausencias
         * estén guardadas antes de generar
         * la respuesta.
         */
        ausenciaRepository.flush();

        /*
         * Las evidencias NO se modifican aquí.
         *
         * Se administran mediante:
         *
         * POST   /{id}/evidencias
         * DELETE /{id}/evidencias/{evidenciaId}
         */
        return obtenerPorId(id);
    }

    /*
     * =========================================================
     * OBTENER ASISTENCIA POR ID
     * =========================================================
     */
    @Transactional(readOnly = true)
    public AsistenciaResponse obtenerPorId(
            Long id
    ) {

        AsistenciaRegistro asistencia =
                asistenciaRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Asistencia no encontrada"
                                )
                        );

        List<AusenciaResponse> ausencias =
                ausenciaRepository
                        .findByAsistenciaId(id)
                        .stream()
                        .map(ausencia ->
                                new AusenciaResponse(
                                        ausencia.getId(),

                                        ausencia
                                                .getTrabajador()
                                                .getId(),

                                        ausencia
                                                .getTrabajador()
                                                .getCodigo(),

                                        ausencia
                                                .getTrabajador()
                                                .getNombreCompleto(),

                                        ausencia
                                                .getMotivo()
                                                .getId(),

                                        ausencia
                                                .getMotivo()
                                                .getNombre(),

                                        ausencia
                                                .getObservacion()
                                )
                        )
                        .toList();

        List<EvidenciaResponse> evidencias =
                evidenciaRepository
                        .findByAsistenciaId(id)
                        .stream()
                        .map(evidencia ->
                                new EvidenciaResponse(
                                        evidencia.getId(),
                                        evidencia.getUrlArchivo(),
                                        evidencia.getTipo()
                                )
                        )
                        .toList();

        return new AsistenciaResponse(
                asistencia.getId(),

                asistencia
                        .getPlaza()
                        .getId(),

                asistencia
                        .getPlaza()
                        .getCodigo(),

                asistencia
                        .getTurno()
                        .getId(),

                asistencia
                        .getTurno()
                        .getCodigo(),

                asistencia
                        .getControlador()
                        .getId(),

                asistencia
                        .getControlador()
                        .getNombreCompleto(),

                asistencia.getFecha(),

                asistencia.getProgramados(),

                asistencia.getPresentes(),

                asistencia.getProgramados()
                        - asistencia.getPresentes(),

                asistencia.getApoyoSolicitado(),

                asistencia.getDetalleApoyo(),

                asistencia.getPorcentaje(),

                asistencia.getNotas(),

                ausencias,

                evidencias
        );
    }

    /*
     * =========================================================
     * LISTAR ASISTENCIAS
     * =========================================================
     *
     * Si no se mandan fechas:
     * inicio = hoy
     * fin = hoy
     *
     * plazaId null:
     * todas las plazas.
     */
    @Transactional(readOnly = true)
    public List<AsistenciaResponse> listar(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId
    ) {

        LocalDate hoy =
                LocalDate.now();

        LocalDate fechaInicio =
                inicio != null
                        ? inicio
                        : hoy;

        LocalDate fechaFin =
                fin != null
                        ? fin
                        : hoy;

        /*
         * Evitar rangos incorrectos.
         */
        if (fechaInicio.isAfter(fechaFin)) {
            throw new BusinessException(
                    "La fecha inicial no puede ser posterior a la fecha final"
            );
        }

        /*
         * La consulta principal trae asistencia + plaza + turno + controlador
         * en una sola consulta mediante JOIN FETCH.
         */
        List<AsistenciaRegistro> registros;

        if (plazaId == null) {

            registros =
                    asistenciaRepository
                            .listarHistorial(
                                    fechaInicio,
                                    fechaFin
                            );

        } else {

            /*
             * Validar que la plaza exista.
             */
            if (!plazaRepository.existsById(plazaId)) {
                throw new ResourceNotFoundException(
                        "Plaza no encontrada"
                );
            }

            registros =
                    asistenciaRepository
                            .listarHistorialPorPlaza(
                                    fechaInicio,
                                    fechaFin,
                                    plazaId
                            );
        }

        if (registros.isEmpty()) {
            return List.of();
        }

        List<Long> ids =
                registros
                        .stream()
                        .map(AsistenciaRegistro::getId)
                        .toList();

        /*
         * Todas las ausencias del historial se cargan en una sola consulta.
         * findAllForHistorial() hace JOIN FETCH de:
         *
         * - asistencia
         * - trabajador
         * - motivo
         *
         * De esta forma no aparecen consultas lazy adicionales.
         */
        Map<Long, List<AusenciaResponse>> ausenciasPorAsistencia =
                ausenciaRepository
                        .findAllForHistorial(ids)
                        .stream()
                        .collect(
                                Collectors.groupingBy(
                                        ausencia ->
                                                ausencia
                                                        .getAsistencia()
                                                        .getId(),

                                        Collectors.mapping(
                                                ausencia ->
                                                        new AusenciaResponse(
                                                                ausencia.getId(),
                                                                ausencia.getTrabajador().getId(),
                                                                ausencia.getTrabajador().getCodigo(),
                                                                ausencia.getTrabajador().getNombreCompleto(),
                                                                ausencia.getMotivo().getId(),
                                                                ausencia.getMotivo().getNombre(),
                                                                ausencia.getObservacion()
                                                        ),
                                                Collectors.toList()
                                        )
                                )
                        );

        /*
         * Todas las evidencias se cargan en una sola consulta.
         *
         * No se utiliza CompletableFuture porque las operaciones JPA deben
         * permanecer dentro del mismo contexto transaccional de Spring.
         */
        Map<Long, List<EvidenciaResponse>> evidenciasPorAsistencia =
                evidenciaRepository
                        .findByAsistenciaIdIn(ids)
                        .stream()
                        .collect(
                                Collectors.groupingBy(
                                        evidencia ->
                                                evidencia
                                                        .getAsistencia()
                                                        .getId(),

                                        Collectors.mapping(
                                                evidencia ->
                                                        new EvidenciaResponse(
                                                                evidencia.getId(),
                                                                evidencia.getUrlArchivo(),
                                                                evidencia.getTipo()
                                                        ),
                                                Collectors.toList()
                                        )
                                )
                        );

        /*
         * Construir la respuesta sin realizar nuevas consultas.
         */
        return registros
                .stream()
                .map(registro ->
                        new AsistenciaResponse(
                                registro.getId(),

                                registro.getPlaza().getId(),
                                registro.getPlaza().getCodigo(),

                                registro.getTurno().getId(),
                                registro.getTurno().getCodigo(),

                                registro.getControlador().getId(),
                                registro.getControlador().getNombreCompleto(),

                                registro.getFecha(),

                                registro.getProgramados(),
                                registro.getPresentes(),

                                registro.getProgramados()
                                        - registro.getPresentes(),

                                registro.getApoyoSolicitado(),
                                registro.getDetalleApoyo(),

                                registro.getPorcentaje(),

                                registro.getNotas(),

                                ausenciasPorAsistencia.getOrDefault(
                                        registro.getId(),
                                        List.of()
                                ),

                                evidenciasPorAsistencia.getOrDefault(
                                        registro.getId(),
                                        List.of()
                                )
                        )
                )
                .toList();
    }

    /*
     * =========================================================
     * LIMPIAR TEXTO
     * =========================================================
     */
    private String limpiarTexto(
            String texto
    ) {

        if (texto == null) {
            return null;
        }

        String limpio =
                texto.trim();

        return limpio.isEmpty()
                ? null
                : limpio;
    }

    /*
     * =========================================================
     * VALIDAR CONTROLADOR
     * =========================================================
     */
    private void validarControlador(
            Trabajador trabajador
    ) {

        if (!Boolean.TRUE.equals(
                trabajador.getActivo()
        )) {
            throw new BusinessException(
                    "El controlador está inactivo"
            );
        }

        String puesto =
                trabajador.getPuesto() == null
                        ? ""
                        : trabajador
                        .getPuesto()
                        .getNombre();

        boolean puestoValido =
                puesto.equalsIgnoreCase(
                        "Controlador"
                )
                        || puesto.equalsIgnoreCase(
                        "Controlador ATF"
                )
                        || puesto.equalsIgnoreCase(
                        "Supervisor"
                );

        if (!puestoValido) {
            throw new BusinessException(
                    "El responsable debe ser Controlador, Controlador ATF o Supervisor"
            );
        }
    }
}
