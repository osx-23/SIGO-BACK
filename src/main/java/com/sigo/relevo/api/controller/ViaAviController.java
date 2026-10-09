package com.sigo.relevo.api.controller;

import com.sigo.relevo.api.dto.ViaResponse;
import com.sigo.relevo.application.port.in.ListarViasUseCase;
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

    @GetMapping
    public List<ViaResponse> listarVisibles(
            @RequestParam Long plazaId
    ) {
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
