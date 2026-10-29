package com.sigo.prediccion.api.controller;

import com.sigo.prediccion.api.dto.ProyeccionFinMesResponse;
import com.sigo.prediccion.infrastructure.ProyeccionFinMesService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/dashboard/asistencia")
@RequiredArgsConstructor
public class ProyeccionFinMesController {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final ProyeccionFinMesService service;

    @GetMapping("/proyeccion-fin-mes")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'CONTROLADOR')")
    public ProyeccionFinMesResponse proyectar(
            @RequestParam Long plazaId,
            @RequestParam(required = false) Long turnoId
    ) {
        return service.proyectar(plazaId, turnoId, LocalDate.now(LIMA));
    }
}
