package com.sigo.relevo.controller;

import com.sigo.personal.entity.RolSistema;
import com.sigo.personal.entity.Trabajador;
import com.sigo.relevo.dto.*;
import com.sigo.relevo.service.RelevoHistorialAccesoService;
import com.sigo.relevo.service.RelevoService;
import com.sigo.security.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/relevos")
@RequiredArgsConstructor
public class RelevoController {

    private final RelevoService service;
    private final RelevoHistorialAccesoService historialAccesoService;
    private final CurrentUserService currentUserService;

    @GetMapping("/elementos")
    public List<ElementoRelevoResponse> elementos() {
        return service.listarElementos();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RelevoResponse registrar(@Valid @RequestBody RelevoRequest request) {
        return service.registrar(asegurarIdentidadOperador(request));
    }

    @PutMapping("/{id}")
    public RelevoResponse actualizar(@PathVariable Long id, @Valid @RequestBody RelevoRequest request) {
        Trabajador actual = currentUserService.requireCurrent();
        if (actual.getRolSistema() == RolSistema.OPERADOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Los operadores solo pueden consultar el historial de relevos");
        }
        return service.actualizar(id, request);
    }

    @GetMapping("/{id}")
    public RelevoResponse obtener(@PathVariable Long id) {
        Trabajador actual = currentUserService.requireCurrent();
        return historialAccesoService.obtenerPara(actual, id);
    }

    @GetMapping
    public List<RelevoResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        Trabajador actual = currentUserService.requireCurrent();
        return historialAccesoService.listarPara(actual, inicio, fin);
    }

    @PostMapping(value = "/checklist/{checklistId}/evidencias", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public EvidenciaRelevoResponse evidenciaChecklist(@PathVariable Long checklistId, @RequestParam("file") MultipartFile file) throws IOException {
        return service.subirEvidenciaChecklist(checklistId, file);
    }

    @DeleteMapping("/checklist/{checklistId}/evidencias/{evidenciaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarEvidenciaChecklist(@PathVariable Long checklistId, @PathVariable Long evidenciaId) throws IOException {
        service.eliminarEvidenciaChecklist(checklistId, evidenciaId);
    }

    @PostMapping(value = "/vias/{relevoViaId}/evidencias", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public EvidenciaRelevoResponse evidenciaVia(@PathVariable Long relevoViaId, @RequestParam("file") MultipartFile file) throws IOException {
        return service.subirEvidenciaVia(relevoViaId, file);
    }

    @DeleteMapping("/vias/{relevoViaId}/evidencias/{evidenciaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarEvidenciaVia(@PathVariable Long relevoViaId, @PathVariable Long evidenciaId) throws IOException {
        service.eliminarEvidenciaVia(relevoViaId, evidenciaId);
    }

    private RelevoRequest asegurarIdentidadOperador(RelevoRequest request) {
        Trabajador actual = currentUserService.requireCurrent();

        if (actual.getRolSistema() != RolSistema.OPERADOR) {
            return request;
        }

        if (actual.getPlaza() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El operador no tiene una plaza asignada"
            );
        }

        return new RelevoRequest(
                actual.getPlaza().getId(),
                request.turnoId(),
                actual.getId(),
                request.fecha(),
                request.hora(),
                request.observaciones(),
                request.resumen(),
                request.checklist(),
                request.vias()
        );
    }
}
