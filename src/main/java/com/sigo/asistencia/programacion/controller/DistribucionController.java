package com.sigo.asistencia.programacion.controller;

import com.sigo.asistencia.programacion.service.ProgramacionService;
import com.sigo.asistencia.programacion.service.ProgramacionService.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/distribucion")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPERVISOR','CONTROLADOR')")
public class DistribucionController {

    private final ProgramacionService service;

    @GetMapping("/ubicaciones")
    public List<UbicacionResponse> ubicaciones(
            @RequestParam Long plazaId
    ) {
        return service.ubicaciones(
                plazaId
        );
    }

    @GetMapping("/ubicaciones/configuracion")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public List<UbicacionResponse> ubicacionesConfiguracion(
            @RequestParam Long plazaId
    ) {
        return service.ubicacionesConfiguracion(
                plazaId
        );
    }

    @PostMapping("/ubicaciones")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public UbicacionResponse crearUbicacion(
            @Valid
            @RequestBody
            GuardarUbicacionRequest request
    ) {
        return service.crearUbicacion(
                request
        );
    }

    @PutMapping("/ubicaciones/{ubicacionId}")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public UbicacionResponse actualizarUbicacion(
            @PathVariable Long ubicacionId,
            @Valid
            @RequestBody
            GuardarUbicacionRequest request
    ) {
        return service.actualizarUbicacion(
                ubicacionId,
                request
        );
    }

    @PatchMapping("/ubicaciones/{ubicacionId}/estado")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public UbicacionResponse cambiarEstadoUbicacion(
            @PathVariable Long ubicacionId,
            @RequestParam boolean activo
    ) {
        return service.cambiarEstadoUbicacion(
                ubicacionId,
                activo
        );
    }

    @GetMapping
    public List<DistribucionDiaResponse> listar(
            @RequestParam Long plazaId,
            @RequestParam int anio,
            @RequestParam int mes
    ) {
        return service.listarDistribucion(
                plazaId,
                anio,
                mes
        );
    }

    @PutMapping
    public List<DistribucionDiaResponse> guardar(
            @Valid
            @RequestBody
            GuardarDistribucionRequest request
    ) {
        return service.guardarDistribucion(
                request
        );
    }

    @GetMapping("/resumen-trabajador/{trabajadorId}")
    public ResumenTrabajadorResponse resumen(
            @PathVariable Long trabajadorId,
            @RequestParam int anio,
            @RequestParam int mes
    ) {
        return service.resumen(
                trabajadorId,
                anio,
                mes
        );
    }

    @GetMapping("/cobertura")
    public List<CoberturaUbicacionResponse> cobertura(
            @RequestParam Long plazaId,
            @RequestParam int anio,
            @RequestParam int mes
    ) {
        return service.cobertura(
                plazaId,
                anio,
                mes
        );
    }
}
