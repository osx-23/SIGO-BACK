package com.sigo.avi.api.controller;

import com.sigo.avi.api.dto.AviRegistroRequest;
import com.sigo.avi.api.dto.AviRegistroResponse;
import com.sigo.avi.application.port.in.AviRegistroUseCase;
import com.sigo.avi.domain.AviAccion;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/avi/registros")
@RequiredArgsConstructor
public class AviRegistroController {

    private final AviRegistroUseCase useCase;

    @PostMapping
    public AviRegistroResponse registrar(
            @Valid @RequestBody AviRegistroRequest request
    ) {
        return map(
                useCase.registrar(
                        new AviRegistroUseCase.Command(
                                request.id(),
                                request.placa(),
                                request.via(),
                                request.accion(),
                                request.fechaHoraEvento(),
                                request.textoReconocido()
                        )
                )
        );
    }

    @GetMapping
    public List<AviRegistroResponse> listar(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime desde,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime hasta,

            @RequestParam(required = false)
            Long plazaId,

            @RequestParam(required = false)
            Integer via,

            @RequestParam(required = false)
            AviAccion accion
    ) {
        return useCase.listar(
                        desde,
                        hasta,
                        plazaId,
                        via,
                        accion
                )
                .stream()
                .map(this::map)
                .toList();
    }

    private AviRegistroResponse map(
            AviRegistroUseCase.Registro item
    ) {
        return new AviRegistroResponse(
                item.id(),
                item.usuarioId(),
                item.usuarioCodigo(),
                item.usuarioNombre(),
                item.plazaId(),
                item.plazaCodigo(),
                item.placa(),
                item.via(),
                item.accion(),
                item.fechaHoraEvento(),
                item.fechaHoraRecepcion(),
                item.textoReconocido()
        );
    }
}
