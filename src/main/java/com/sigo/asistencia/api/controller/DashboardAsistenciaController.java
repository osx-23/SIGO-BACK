package com.sigo.asistencia.api.controller;

import com.sigo.asistencia.api.dto.AusenciaMotivoResponse;
import com.sigo.asistencia.api.dto.DashboardPuntoResponse;
import com.sigo.asistencia.api.dto.ResumenAsistenciaResponse;
import com.sigo.asistencia.application.port.in.DashboardAsistenciaUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard/asistencia")
@RequiredArgsConstructor
public class DashboardAsistenciaController {

    private final DashboardAsistenciaUseCase useCase;

    @GetMapping("/diario")
    public ResponseEntity<List<DashboardPuntoResponse>> diario(
            @RequestParam int anio,
            @RequestParam int mes,
            @RequestParam(required = false) Long plazaId,
            @RequestParam(required = false) Long turnoId
    ) {
        return ResponseEntity.ok(
                useCase.diario(anio, mes, plazaId, turnoId)
                        .stream()
                        .map(this::toPuntoResponse)
                        .toList()
        );
    }

    @GetMapping("/anual")
    public ResponseEntity<List<DashboardPuntoResponse>> anual(
            @RequestParam int anio,
            @RequestParam(required = false) Long plazaId,
            @RequestParam(required = false) Long turnoId
    ) {
        return ResponseEntity.ok(
                useCase.anual(anio, plazaId, turnoId)
                        .stream()
                        .map(this::toPuntoResponse)
                        .toList()
        );
    }

    @GetMapping("/ausencias-motivo")
    public ResponseEntity<List<AusenciaMotivoResponse>> motivos(
            @RequestParam int anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Long plazaId,
            @RequestParam(required = false) Long turnoId
    ) {
        return ResponseEntity.ok(
                useCase.motivos(anio, mes, plazaId, turnoId)
                        .stream()
                        .map(item ->
                                new AusenciaMotivoResponse(
                                        item.motivo(),
                                        item.total()
                                )
                        )
                        .toList()
        );
    }

    @GetMapping("/resumen")
    public ResponseEntity<ResumenAsistenciaResponse> resumen(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate inicio,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fin,
            @RequestParam(required = false) Long plazaId,
            @RequestParam(required = false) Long turnoId
    ) {
        DashboardAsistenciaUseCase.Resumen resumen =
                useCase.resumen(
                        inicio,
                        fin,
                        plazaId,
                        turnoId
                );

        return ResponseEntity.ok(
                new ResumenAsistenciaResponse(
                        resumen.registros(),
                        resumen.presentes(),
                        resumen.programados(),
                        resumen.ausentes(),
                        resumen.porcentajeGeneral()
                )
        );
    }

    private DashboardPuntoResponse toPuntoResponse(
            DashboardAsistenciaUseCase.Punto punto
    ) {
        return new DashboardPuntoResponse(
                punto.periodo(),
                punto.presentes(),
                punto.programados(),
                punto.porcentaje()
        );
    }
}
