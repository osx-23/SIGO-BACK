package com.sigo.programacion.api.controller;

import com.sigo.programacion.application.port.in.GeneradorAsignacionCasetasUseCase;
import com.sigo.programacion.domain.GrupoFlujoCaseta;
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

    private final GeneradorAsignacionCasetasUseCase service;

    @GetMapping("/configuracion")
    public GeneradorAsignacionCasetasUseCase.Configuracion configuracion(
            @RequestParam Long plazaId
    ) {
        return service.obtenerConfiguracion(plazaId);
    }

    @PutMapping("/configuracion")
    public GeneradorAsignacionCasetasUseCase.Configuracion guardarConfiguracion(
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
    public List<GeneradorAsignacionCasetasUseCase.CasetaConfig> casetas(
            @RequestParam Long plazaId
    ) {
        return service.listarCasetas(plazaId);
    }

    @PutMapping("/casetas/{ubicacionId}")
    public GeneradorAsignacionCasetasUseCase.CasetaConfig guardarCaseta(
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
    public List<GeneradorAsignacionCasetasUseCase.Restriccion> restricciones(
            @RequestParam Long plazaId
    ) {
        return service.listarRestricciones(plazaId);
    }

    @PutMapping("/restricciones")
    public GeneradorAsignacionCasetasUseCase.Restriccion guardarRestriccion(
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
    public GeneradorAsignacionCasetasUseCase.Propuesta generar(
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
            @NotNull GeneradorAsignacionCasetasUseCase.TipoPeriodo periodo,
            Integer semana
    ) {}
}
