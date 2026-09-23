package com.sigo.asistencia.api.controller;

import com.sigo.asistencia.api.dto.AsistenciaRequest;
import com.sigo.asistencia.api.dto.AsistenciaResponse;
import com.sigo.asistencia.api.dto.AsistenciaUpdateRequest;
import com.sigo.asistencia.api.dto.EvidenciaResponse;
import com.sigo.asistencia.application.service.AsistenciaExcepcionService;
import com.sigo.asistencia.application.port.in.ConsultarAsistenciasUseCase;
import com.sigo.asistencia.application.port.in.GestionarAsistenciaUseCase;
import com.sigo.asistencia.application.port.in.GestionarEvidenciaAsistenciaUseCase;
import com.sigo.asistencia.application.port.in.ObtenerProgramadosAsistenciaUseCase;
import com.sigo.personal.infrastructure.persistence.entity.RolSistema;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.security.application.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/asistencias")
@RequiredArgsConstructor
public class AsistenciaController {

    private final GestionarAsistenciaUseCase gestionarUseCase;
    private final GestionarEvidenciaAsistenciaUseCase evidenciaUseCase;
    private final ConsultarAsistenciasUseCase consultaUseCase;
    private final AsistenciaExcepcionService asistenciaExcepcionService;
    private final ObtenerProgramadosAsistenciaUseCase programacionService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<AsistenciaResponse> registrar(
            @Valid @RequestBody AsistenciaRequest request,
            @RequestParam(defaultValue = "false") boolean excepcionControlador
    ) {
        exigirRolAsistencia();

        if (excepcionControlador) {
            return ResponseEntity.ok(
                    asistenciaExcepcionService.registrar(request)
            );
        }

        asegurarIdentidad(request);

        return ResponseEntity.ok(
                toResponse(
                        gestionarUseCase.registrar(
                                toCommand(request)
                        )
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<AsistenciaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody AsistenciaUpdateRequest request
    ) {
        asegurarIdentidad(request);

        return ResponseEntity.ok(
                toResponse(
                        gestionarUseCase.actualizar(
                                id,
                                toCommand(request)
                        )
                )
        );
    }

    /**
     * Devuelve la cantidad programada sugerida para la combinación plaza + turno.
     * Este valor se usa como autocompletado en el frontend, pero puede ser ajustado
     * manualmente antes de registrar o actualizar la asistencia.
     */
    @GetMapping("/programados")
    public ResponseEntity<Map<String, Integer>> programados(
            @RequestParam Long plazaId,
            @RequestParam Long turnoId
    ) {
        exigirRolAsistencia();
        return ResponseEntity.ok(Map.of(
                "programados",
                programacionService.obtenerProgramados(plazaId, turnoId)
        ));
    }

    @GetMapping
    public ResponseEntity<List<AsistenciaResponse>> listar(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate inicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fin,
            @RequestParam(required = false)
            Long plazaId
    ) {
        return ResponseEntity.ok(
                consultaUseCase
                        .listar(inicio, fin, plazaId)
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<AsistenciaResponse> obtenerPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                toResponse(
                        consultaUseCase.obtenerPorId(id)
                )
        );
    }

    @PostMapping(value = "/{id}/evidencias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EvidenciaResponse> subirEvidencia(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("tipo") String tipo
    ) throws IOException {
        exigirRolAsistencia();
        var evidencia = evidenciaUseCase.guardar(
                id,
                new GestionarEvidenciaAsistenciaUseCase.ArchivoEntrada(
                        file.getOriginalFilename(),
                        file.getContentType(),
                        file.getBytes()
                ),
                tipo
        );
        return ResponseEntity.ok(
                new EvidenciaResponse(
                        evidencia.id(),
                        evidencia.urlArchivo(),
                        evidencia.tipo()
                )
        );
    }

    @DeleteMapping("/{asistenciaId}/evidencias/{evidenciaId}")
    public ResponseEntity<Void> eliminarEvidencia(
            @PathVariable Long asistenciaId,
            @PathVariable Long evidenciaId
    ) throws IOException {
        exigirRolAsistencia();
        evidenciaUseCase.eliminar(asistenciaId, evidenciaId);
        return ResponseEntity.noContent().build();
    }

    private GestionarAsistenciaUseCase.Command toCommand(
            AsistenciaRequest request
    ) {
        return new GestionarAsistenciaUseCase.Command(
                request.plazaId(),
                request.turnoId(),
                request.controladorId(),
                request.fecha(),
                request.programados(),
                request.presentes(),
                request.apoyoSolicitado(),
                request.detalleApoyo(),
                request.notas(),
                request.ausencias() == null
                        ? List.of()
                        : request.ausencias()
                                .stream()
                                .map(ausencia ->
                                        new GestionarAsistenciaUseCase.AusenciaCommand(
                                                ausencia.trabajadorId(),
                                                ausencia.motivoId(),
                                                ausencia.observacion()
                                        )
                                )
                                .toList(),
                request.evidencias()
        );
    }

    private GestionarAsistenciaUseCase.Command toCommand(
            AsistenciaUpdateRequest request
    ) {
        return new GestionarAsistenciaUseCase.Command(
                request.plazaId(),
                request.turnoId(),
                request.controladorId(),
                request.fecha(),
                request.programados(),
                request.presentes(),
                request.apoyoSolicitado(),
                request.detalleApoyo(),
                request.notas(),
                request.ausencias() == null
                        ? List.of()
                        : request.ausencias()
                                .stream()
                                .map(ausencia ->
                                        new GestionarAsistenciaUseCase.AusenciaCommand(
                                                ausencia.trabajadorId(),
                                                ausencia.motivoId(),
                                                ausencia.observacion()
                                        )
                                )
                                .toList(),
                List.of()
        );
    }

    private AsistenciaResponse toResponse(
            ConsultarAsistenciasUseCase.Asistencia asistencia
    ) {
        return new AsistenciaResponse(
                asistencia.id(),
                asistencia.plazaId(),
                asistencia.plaza(),
                asistencia.turnoId(),
                asistencia.turno(),
                asistencia.controladorId(),
                asistencia.controlador(),
                asistencia.fecha(),
                asistencia.programados(),
                asistencia.presentes(),
                asistencia.ausentes(),
                asistencia.apoyoSolicitado(),
                asistencia.detalleApoyo(),
                asistencia.porcentaje(),
                asistencia.notas(),
                asistencia.ausencias()
                        .stream()
                        .map(ausencia ->
                                new com.sigo.asistencia.api.dto.AusenciaResponse(
                                        ausencia.id(),
                                        ausencia.trabajadorId(),
                                        ausencia.codigoTrabajador(),
                                        ausencia.nombreTrabajador(),
                                        ausencia.motivoId(),
                                        ausencia.motivo(),
                                        ausencia.observacion()
                                )
                        )
                        .toList(),
                asistencia.evidencias()
                        .stream()
                        .map(evidencia ->
                                new EvidenciaResponse(
                                        evidencia.id(),
                                        evidencia.urlArchivo(),
                                        evidencia.tipo()
                                )
                        )
                        .toList()
        );
    }

    private AsistenciaRequest asegurarIdentidad(AsistenciaRequest request) {
        // Supervisores y controladores pueden registrar asistencia en cualquier plaza.
        // La plaza válida es la seleccionada explícitamente en el formulario.
        exigirRolAsistencia();
        return request;
    }

    private AsistenciaUpdateRequest asegurarIdentidad(AsistenciaUpdateRequest request) {
        // Supervisores y controladores pueden actualizar registros de cualquier plaza.
        // No se reemplaza plazaId por la plaza asignada al usuario autenticado.
        exigirRolAsistencia();
        return request;
    }

    private Trabajador exigirRolAsistencia() {
        Trabajador actual = currentUserService.requireCurrent();
        if (actual.getRolSistema() != RolSistema.SUPERVISOR
                && actual.getRolSistema() != RolSistema.CONTROLADOR) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo supervisores y controladores pueden gestionar asistencia"
            );
        }
        return actual;
    }
}
