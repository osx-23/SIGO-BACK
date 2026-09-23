package com.sigo.asistencia.controller;

import com.sigo.asistencia.dto.AusenciaMotivoResponse;
import com.sigo.asistencia.dto.DashboardPuntoResponse;
import com.sigo.asistencia.dto.ResumenAsistenciaResponse;
import com.sigo.asistencia.service.DashboardAsistenciaService;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard/asistencia")
@RequiredArgsConstructor
public class DashboardAsistenciaController {

    private final DashboardAsistenciaService service;

    /*
     * ============================================================
     * DASHBOARD DIARIO
     * ============================================================
     *
     * Ejemplos:
     *
     * /api/dashboard/asistencia/diario?anio=2026&mes=9
     *
     * /api/dashboard/asistencia/diario
     * ?anio=2026
     * &mes=9
     * &plazaId=4
     * &turnoId=1
     */

    @GetMapping("/diario")
    public ResponseEntity<List<DashboardPuntoResponse>> diario(
            @RequestParam int anio,
            @RequestParam int mes,
            @RequestParam(required = false) Long plazaId,
            @RequestParam(required = false) Long turnoId
    ) {

        List<DashboardPuntoResponse> resultado =
                service.diario(
                        anio,
                        mes,
                        plazaId,
                        turnoId
                );

        return ResponseEntity.ok(resultado);
    }

    /*
     * ============================================================
     * DASHBOARD ANUAL
     * ============================================================
     *
     * Ejemplos:
     *
     * /api/dashboard/asistencia/anual?anio=2026
     *
     * /api/dashboard/asistencia/anual
     * ?anio=2026
     * &plazaId=4
     * &turnoId=1
     */

    @GetMapping("/anual")
    public ResponseEntity<List<DashboardPuntoResponse>> anual(
            @RequestParam int anio,
            @RequestParam(required = false) Long plazaId,
            @RequestParam(required = false) Long turnoId
    ) {

        List<DashboardPuntoResponse> resultado =
                service.anual(
                        anio,
                        plazaId,
                        turnoId
                );

        return ResponseEntity.ok(resultado);
    }

    /*
     * ============================================================
     * AUSENCIAS POR MOTIVO
     * ============================================================
     *
     * Si se envía mes:
     *
     * /api/dashboard/asistencia/ausencias-motivo
     * ?anio=2026
     * &mes=9
     *
     * Si NO se envía mes:
     *
     * /api/dashboard/asistencia/ausencias-motivo
     * ?anio=2026
     *
     * En ese caso devuelve todo el año.
     *
     * También acepta:
     *
     * &plazaId=4
     * &turnoId=1
     */

    @GetMapping("/ausencias-motivo")
    public ResponseEntity<List<AusenciaMotivoResponse>> motivos(
            @RequestParam int anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Long plazaId,
            @RequestParam(required = false) Long turnoId
    ) {

        List<AusenciaMotivoResponse> resultado =
                service.motivos(
                        anio,
                        mes,
                        plazaId,
                        turnoId
                );

        return ResponseEntity.ok(resultado);
    }

    /*
     * ============================================================
     * RESUMEN DE ASISTENCIA
     * ============================================================
     *
     * Ejemplo:
     *
     * /api/dashboard/asistencia/resumen
     * ?inicio=2026-09-01
     * &fin=2026-09-30
     *
     * Con filtros:
     *
     * /api/dashboard/asistencia/resumen
     * ?inicio=2026-09-01
     * &fin=2026-09-30
     * &plazaId=4
     * &turnoId=1
     *
     * Se utiliza para:
     *
     * - resumen semanal
     * - resumen mensual
     * - resumen anual
     * - comparación por turno
     * - comparación con mes anterior
     */

    @GetMapping("/resumen")
    public ResponseEntity<ResumenAsistenciaResponse> resumen(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate inicio,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fin,

            @RequestParam(required = false)
            Long plazaId,

            @RequestParam(required = false)
            Long turnoId
    ) {

        ResumenAsistenciaResponse resultado =
                service.resumen(
                        inicio,
                        fin,
                        plazaId,
                        turnoId
                );

        return ResponseEntity.ok(resultado);
    }
}