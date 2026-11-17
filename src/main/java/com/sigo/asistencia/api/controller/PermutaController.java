package com.sigo.asistencia.api.controller;

import com.sigo.asistencia.application.port.in.PermutaUseCase;
import com.sigo.asistencia.application.port.in.ExportarPermutasExcelUseCase;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final ExportarPermutasExcelUseCase exportador;

    @GetMapping("/agentes/{codigo}")
    public PermutaUseCase.AgenteIdentificado agente(@PathVariable int codigo) {
        return service.agente(codigo);
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

    @GetMapping("/pagina")
    public PermutaUseCase.PaginaPermutas pagina(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Long plazaId,
            @RequestParam(required = false) Integer codigo,
            @RequestParam(required = false) Long turnoId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamano) {
        return service.listarPaginado(inicio, fin, plazaId, codigo, turnoId, pagina, tamano);
    }

    @GetMapping(value = "/exportar", produces =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportar(
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) Long plazaId,
            @RequestParam(required = false) Integer codigo,
            @RequestParam(required = false) Long turnoId) {
        int anioReporte = anio == null ? (inicio == null ? LocalDate.now().getYear() : inicio.getYear()) : anio;
        byte[] excel = exportador.exportar(anioReporte, inicio, fin, plazaId, turnoId, codigo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"SIGO_Permutas_" + anioReporte + ".xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    @GetMapping("/dashboard")
    public PermutaUseCase.DashboardPermutas dashboard(@RequestParam String mes) {
        return service.dashboard(YearMonth.parse(mes));
    }
}
