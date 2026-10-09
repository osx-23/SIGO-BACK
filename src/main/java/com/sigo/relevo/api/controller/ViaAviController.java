package com.sigo.relevo.api.controller;

import com.sigo.relevo.api.dto.ViaResponse;
import com.sigo.relevo.application.port.in.ListarViasUseCase;
import com.sigo.security.application.port.in.UsuarioActualUseCase;
import com.sigo.shared.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/avi/vias")
@RequiredArgsConstructor
public class ViaAviController {

    private final ListarViasUseCase useCase;
    private final UsuarioActualUseCase usuarioActualUseCase;

    @GetMapping
    public List<ViaResponse> listarVisibles(
            @RequestParam Long plazaId
    ) {
        UsuarioActualUseCase.UsuarioActual actual =
                usuarioActualUseCase.requireActual();

        if (
                actual.plazaId() == null ||
                !actual.plazaId().equals(plazaId)
        ) {
            throw new ForbiddenException(
                    "AVIX solo puede consultar las vías de la plaza asignada al usuario"
            );
        }

        return useCase
                .listarVisiblesAvi(plazaId)
                .stream()
                .map(via ->
                        new ViaResponse(
                                via.id(),
                                via.plazaId(),
                                via.numero(),
                                via.nombre(),
                                via.activa(),
                                via.orden()
                        )
                )
                .toList();
    }
}
