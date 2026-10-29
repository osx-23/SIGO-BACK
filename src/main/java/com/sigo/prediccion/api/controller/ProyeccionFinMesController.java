package com.sigo.prediccion.api.controller;

import com.sigo.prediccion.api.dto.ProyeccionFinMesResponse;
import com.sigo.prediccion.application.ProyeccionFinMesService;
import com.sigo.prediccion.domain.ProyeccionFinMesResultado;
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
        ProyeccionFinMesResultado r = service.proyectar(plazaId, turnoId, LocalDate.now(LIMA));
        return new ProyeccionFinMesResponse(
                r.plazaId(), r.fechaConsulta(), r.desde(), r.hasta(),
                r.turnosProyectados(), r.turnosSinProgramacion(), r.turnosYaRegistrados(),
                r.totalProgramados(), r.totalAusenciasEstimadas(), r.totalCoberturaEstimada(),
                r.detalle().stream()
                        .map(d -> new ProyeccionFinMesResponse.Detalle(
                                d.fecha(), d.turnoId(), d.turnoCodigo(), d.estado(),
                                d.programados(), d.ausenciasEstimadas(), d.coberturaEstimada()))
                        .toList()
        );
    }
}
