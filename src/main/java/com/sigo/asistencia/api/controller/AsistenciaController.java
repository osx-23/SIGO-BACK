package com.sigo.asistencia.api.controller;

import com.sigo.asistencia.api.dto.AsistenciaRequest;
import com.sigo.asistencia.api.dto.AsistenciaResponse;
import com.sigo.asistencia.api.dto.AsistenciaUpdateRequest;
import com.sigo.asistencia.api.dto.EvidenciaResponse;
import com.sigo.asistencia.application.service.AsistenciaExcepcionService;
import com.sigo.asistencia.application.service.AsistenciaProgramacionService;
import com.sigo.asistencia.application.service.AsistenciaService;
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

    private final AsistenciaService asistenciaService;
    private final AsistenciaExcepcionService asistenciaExcepcionService;
    private final AsistenciaProgramacionService programacionService;
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

        return ResponseEntity.ok(
                asistenciaService.registrar(asegurarIdentidad(request))
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<AsistenciaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody AsistenciaUpdateRequest request
    ) {
        return ResponseEntity.ok(
                asistenciaService.actualizar(id, asegurarIdentidad(request))
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Long plazaId
    ) {
        return ResponseEntity.ok(asistenciaService.listar(inicio, fin, plazaId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AsistenciaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(asistenciaService.obtenerPorId(id));
    }

    @PostMapping(value = "/{id}/evidencias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EvidenciaResponse> subirEvidencia(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("tipo") String tipo
    ) throws IOException {
        exigirRolAsistencia();
        return ResponseEntity.ok(asistenciaService.guardarEvidencia(id, file, tipo));
    }

    @DeleteMapping("/{asistenciaId}/evidencias/{evidenciaId}")
    public ResponseEntity<Void> eliminarEvidencia(
            @PathVariable Long asistenciaId,
            @PathVariable Long evidenciaId
    ) throws IOException {
        exigirRolAsistencia();
        asistenciaService.eliminarEvidencia(asistenciaId, evidenciaId);
        return ResponseEntity.noContent().build();
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
