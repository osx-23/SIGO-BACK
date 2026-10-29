package com.sigo.programacion.api.controller;

import com.sigo.programacion.api.dto.CrearTrasladoPlazaRequest;
import com.sigo.programacion.application.service.TrasladoPlazaService;
import com.sigo.programacion.application.port.out.TrasladoPlazaPort;
import com.sigo.programacion.domain.TrasladoPlaza;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/programacion/traslados")
@PreAuthorize("hasRole('SUPERVISOR')")
@RequiredArgsConstructor
public class TrasladoPlazaController {
    private final TrasladoPlazaService service;

    @GetMapping("/candidatos")
    public List<TrasladoPlazaPort.Candidato> candidatos() {
        return service.candidatos();
    }

    @GetMapping
    public List<TrasladoPlaza> listar(
            @RequestParam(required = false) Long plazaId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes) {
        return service.listar(plazaId, anio, mes);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrasladoPlaza crear(@Valid @RequestBody CrearTrasladoPlazaRequest request) {
        return service.crear(request.trabajadorId(), request.plazaDestinoId(),
                request.fechaInicio(), request.fechaFin());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable Long id) {
        service.cancelar(id);
    }
}
