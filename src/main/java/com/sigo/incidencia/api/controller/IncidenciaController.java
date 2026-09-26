package com.sigo.incidencia.api.controller;

import com.sigo.incidencia.api.dto.IncidenciaRequest;
import com.sigo.incidencia.api.dto.IncidenciaResponse;
import com.sigo.incidencia.application.service.IncidenciaService;
import com.sigo.incidencia.domain.EstadoIncidencia;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidencias")
@RequiredArgsConstructor
public class IncidenciaController {

    private final IncidenciaService service;

    @GetMapping("/tipos")
    public List<IncidenciaService.TipoResumen> tipos() {
        return service.listarTipos();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidenciaResponse registrar(
            @Valid @RequestBody IncidenciaRequest request
    ) {
        return service.registrar(request);
    }

    @GetMapping
    public List<IncidenciaResponse> listar(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate inicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fin,
            @RequestParam(required = false) Long plazaId,
            @RequestParam(required = false) EstadoIncidencia estado
    ) {
        return service.listar(inicio, fin, plazaId, estado);
    }

    @GetMapping("/{id}")
    public IncidenciaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping("/{id}/evidencias")
    @ResponseStatus(HttpStatus.CREATED)
    public IncidenciaResponse.Evidencia subirEvidencia(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        return service.subirEvidencia(id, file);
    }

    @PatchMapping("/{id}/atender")
    @PreAuthorize("hasAnyRole('SUPERVISOR','CONTROLADOR')")
    public IncidenciaResponse atender(@PathVariable Long id) {
        return service.atender(id);
    }

    @GetMapping("/pendientes/count")
    @PreAuthorize("hasAnyRole('SUPERVISOR','CONTROLADOR')")
    public Map<String, Long> pendientes() {
        return Map.of("cantidad", service.contarPendientes());
    }
}
