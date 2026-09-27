package com.sigo.programacion.api.controller;

import com.sigo.programacion.application.service.GeneradorAsignacionCasetasService;
import com.sigo.programacion.infrastructure.persistence.entity.GrupoFlujoCaseta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/distribucion/generador")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERVISOR')")
public class GeneradorAsignacionCasetasController {

    private final GeneradorAsignacionCasetasService service;

    @GetMapping("/configuracion")
    public GeneradorAsignacionCasetasService.Configuracion configuracion(
            @RequestParam Long plazaId
    ) {
        return service.obtenerConfiguracion(plazaId);
    }

    @PutMapping("/configuracion")
    public GeneradorAsignacionCasetasService.Configuracion guardarConfiguracion(
            @Valid
            @RequestBody ConfiguracionRequest request
    ) {
        return service.guardarConfiguracion(
                request.plazaId(),
                request.maxMismaCasetaSemana(),
                request.maxMismaCasetaMes(),
                request.maxConsecutivos(),
                request.balancearFlujo()
        );
    }

    @GetMapping("/casetas")
    public List<GeneradorAsignacionCasetasService.CasetaConfig> casetas(
            @RequestParam Long plazaId
    ) {
        return service.listarCasetas(plazaId);
    }

    @PutMapping("/casetas/{ubicacionId}")
    public GeneradorAsignacionCasetasService.CasetaConfig guardarCaseta(
            @PathVariable Long ubicacionId,
            @Valid
            @RequestBody CasetaRequest request
    ) {
        return service.guardarCaseta(
                request.plazaId(),
                ubicacionId,
                request.grupoFlujo(),
                request.maxSemana(),
                request.maxMes()
        );
    }

    @GetMapping("/restricciones")
    public List<GeneradorAsignacionCasetasService.Restriccion> restricciones(
            @RequestParam Long plazaId
    ) {
        return service.listarRestricciones(plazaId);
    }

    @PutMapping("/restricciones")
    public GeneradorAsignacionCasetasService.Restriccion guardarRestriccion(
            @Valid
            @RequestBody RestriccionRequest request
    ) {
        return service.guardarRestriccion(
                request.plazaId(),
                request.trabajadorId(),
                request.ubicacionId(),
                request.motivo(),
                request.activo()
        );
    }

    @DeleteMapping("/restricciones/{restriccionId}")
    public void eliminarRestriccion(
            @PathVariable Long restriccionId
    ) {
        service.eliminarRestriccion(restriccionId);
    }

    @PostMapping("/propuesta")
    public GeneradorAsignacionCasetasService.Propuesta generar(
            @Valid
            @RequestBody GenerarRequest request
    ) {
        return service.generar(
                request.plazaId(),
                request.anio(),
                request.mes(),
                request.periodo(),
                request.semana()
        );
    }

    public record ConfiguracionRequest(
            @NotNull Long plazaId,
            @Min(1) int maxMismaCasetaSemana,
            @Min(1) int maxMismaCasetaMes,
            @Min(1) int maxConsecutivos,
            boolean balancearFlujo
    ) {}

    public record CasetaRequest(
            @NotNull Long plazaId,
            @NotNull GrupoFlujoCaseta grupoFlujo,
            Integer maxSemana,
            Integer maxMes
    ) {}

    public record RestriccionRequest(
            @NotNull Long plazaId,
            @NotNull Long trabajadorId,
            @NotNull Long ubicacionId,
            String motivo,
            boolean activo
    ) {}

    public record GenerarRequest(
            @NotNull Long plazaId,
            int anio,
            int mes,
            @NotNull GeneradorAsignacionCasetasService.TipoPeriodo periodo,
            Integer semana
    ) {}
}
