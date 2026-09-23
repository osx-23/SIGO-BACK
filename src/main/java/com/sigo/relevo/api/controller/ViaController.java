package com.sigo.relevo.api.controller;

import com.sigo.relevo.api.dto.ViaResponse;
import com.sigo.relevo.application.port.in.ListarViasUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vias")
@RequiredArgsConstructor
public class ViaController {

    private final ListarViasUseCase useCase;

    @GetMapping
    public List<ViaResponse> listarPorPlaza(
            @RequestParam Long plazaId
    ) {
        return useCase
                .listarPorPlaza(plazaId)
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
