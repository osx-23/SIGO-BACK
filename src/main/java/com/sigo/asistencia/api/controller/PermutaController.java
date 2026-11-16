package com.sigo.asistencia.api.controller;

import com.sigo.asistencia.application.port.in.PermutaUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/permutas")
@PreAuthorize("hasAnyRole('SUPERVISOR', 'CONTROLADOR')")
@RequiredArgsConstructor
public class PermutaController {
    private final PermutaUseCase service;

    @GetMapping("/agentes/{codigo}")
    public PermutaUseCase.AgenteProgramado agente(@PathVariable int codigo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.agente(codigo, fecha);
    }

    @GetMapping("/turno")
    public List<PermutaUseCase.PermutaDetalle> turno(
            @RequestParam Long plazaId, @RequestParam Long turnoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.delTurno(plazaId, turnoId, fecha);
    }

    @GetMapping
    public List<PermutaUseCase.PermutaDetalle> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Long plazaId,
            @RequestParam(required = false) Integer codigo,
            @RequestParam(required = false) Long turnoId) {
        return service.listar(inicio, fin, plazaId, codigo, turnoId);
    }

    @GetMapping("/dashboard")
    public PermutaUseCase.DashboardPermutas dashboard(@RequestParam String mes) {
        return service.dashboard(YearMonth.parse(mes));
    }
}
