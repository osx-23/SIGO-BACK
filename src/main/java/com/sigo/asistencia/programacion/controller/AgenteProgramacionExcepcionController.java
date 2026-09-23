package com.sigo.asistencia.programacion.controller;

import com.sigo.asistencia.programacion.dto.AgenteProgramacionExcepcionRequest;
import com.sigo.asistencia.programacion.dto.AgenteProgramacionExcepcionResponse;
import com.sigo.asistencia.programacion.service.AgenteProgramacionExcepcionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/programacion/excepciones")
@RequiredArgsConstructor
public class AgenteProgramacionExcepcionController {

    private final AgenteProgramacionExcepcionService service;

    @GetMapping
    public List<AgenteProgramacionExcepcionResponse> listar(@RequestParam Long plazaId) {
        return service.listarPorPlaza(plazaId);
    }

    @GetMapping("/agente/{trabajadorId}")
    public AgenteProgramacionExcepcionResponse obtener(
            @PathVariable Long trabajadorId,
            @RequestParam Long plazaId
    ) {
        return service.obtener(trabajadorId, plazaId);
    }

    @PutMapping
    public AgenteProgramacionExcepcionResponse guardar(
            @Valid @RequestBody AgenteProgramacionExcepcionRequest request
    ) {
        return service.guardar(request);
    }

    @DeleteMapping("/agente/{trabajadorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(
            @PathVariable Long trabajadorId,
            @RequestParam Long plazaId
    ) {
        service.desactivar(trabajadorId, plazaId);
    }
}
