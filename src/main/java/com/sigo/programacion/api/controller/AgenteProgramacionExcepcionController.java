package com.sigo.programacion.api.controller;

import com.sigo.programacion.api.dto.AgenteProgramacionExcepcionRequest;
import com.sigo.programacion.api.dto.AgenteProgramacionExcepcionResponse;
import com.sigo.programacion.application.port.in.AgenteProgramacionExcepcionUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/programacion/excepciones")
@RequiredArgsConstructor
public class AgenteProgramacionExcepcionController {

    private final AgenteProgramacionExcepcionUseCase useCase;

    @GetMapping
    public List<AgenteProgramacionExcepcionResponse> listar(
            @RequestParam Long plazaId
    ) {
        return useCase
                .listarPorPlaza(plazaId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/agente/{trabajadorId}")
    public AgenteProgramacionExcepcionResponse obtener(
            @PathVariable Long trabajadorId,
            @RequestParam Long plazaId
    ) {
        return toResponse(
                useCase.obtener(
                        trabajadorId,
                        plazaId
                )
        );
    }

    @PutMapping
    public AgenteProgramacionExcepcionResponse guardar(
            @Valid
            @RequestBody AgenteProgramacionExcepcionRequest request
    ) {
        return toResponse(
                useCase.guardar(
                        new AgenteProgramacionExcepcionUseCase.Command(
                                request.trabajadorId(),
                                request.plazaId(),
                                request.permiteA(),
                                request.permiteB(),
                                request.permiteC(),
                                request.motivo(),
                                request.color(),
                                request.activo()
                        )
                )
        );
    }

    @DeleteMapping("/agente/{trabajadorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(
            @PathVariable Long trabajadorId,
            @RequestParam Long plazaId
    ) {
        useCase.desactivar(
                trabajadorId,
                plazaId
        );
    }

    private AgenteProgramacionExcepcionResponse toResponse(
            AgenteProgramacionExcepcionUseCase.Excepcion excepcion
    ) {
        return new AgenteProgramacionExcepcionResponse(
                excepcion.id(),
                excepcion.trabajadorId(),
                excepcion.codigoTrabajador(),
                excepcion.nombreTrabajador(),
                excepcion.plazaId(),
                excepcion.plazaCodigo(),
                excepcion.permiteA(),
                excepcion.permiteB(),
                excepcion.permiteC(),
                excepcion.motivo(),
                excepcion.color(),
                excepcion.activo(),
                excepcion.createdAt(),
                excepcion.updatedAt()
        );
    }
}
