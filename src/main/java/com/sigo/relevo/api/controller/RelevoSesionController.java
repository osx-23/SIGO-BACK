package com.sigo.relevo.api.controller;

import com.sigo.relevo.api.dto.RelevoRequest;
import com.sigo.relevo.application.port.in.RelevoSesionUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/relevos/sesiones")
@RequiredArgsConstructor
public class RelevoSesionController {
    private final RelevoSesionUseCase sesiones;

    public record InicioRequest(@NotNull Long plazaId, @NotNull Long turnoId) {}
    public record BorradorRequest(@NotNull String borrador) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RelevoSesionUseCase.Sesion iniciar(@Valid @RequestBody InicioRequest request) {
        return sesiones.iniciar(request.plazaId(), request.turnoId());
    }

    @GetMapping("/actual")
    public RelevoSesionUseCase.Sesion actual() {
        return sesiones.actual();
    }

    @PutMapping("/{id}/borrador")
    public RelevoSesionUseCase.Sesion guardar(
            @PathVariable Long id, @Valid @RequestBody BorradorRequest request) {
        return sesiones.guardarBorrador(id, request.borrador());
    }

    @PostMapping("/{id}/finalizar")
    public RelevoSesionUseCase.Sesion finalizar(
            @PathVariable Long id, @Valid @RequestBody RelevoRequest request) {
        return sesiones.finalizar(id, RelevoController.toCommand(request));
    }

    @PostMapping("/{id}/anular")
    public RelevoSesionUseCase.Sesion anular(@PathVariable Long id) {
        return sesiones.cancelar(id);
    }
}
