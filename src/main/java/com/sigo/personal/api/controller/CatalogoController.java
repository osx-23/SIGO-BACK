package com.sigo.personal.api.controller;

import com.sigo.asistencia.application.port.in.MotivoAusenciaCatalogoUseCase;
import com.sigo.personal.api.dto.MotivoAusenciaCatalogoResponse;
import com.sigo.personal.api.dto.PlazaCatalogoResponse;
import com.sigo.personal.api.dto.TurnoCatalogoResponse;
import com.sigo.personal.application.port.in.CatalogoPersonalUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoPersonalUseCase catalogoPersonalUseCase;
    private final MotivoAusenciaCatalogoUseCase motivoAusenciaUseCase;

    @GetMapping("/plazas")
    public List<PlazaCatalogoResponse> plazas() {
        return catalogoPersonalUseCase
                .plazas()
                .stream()
                .map(item ->
                        new PlazaCatalogoResponse(
                                item.id(),
                                item.codigo(),
                                item.descripcion(),
                                item.activo()
                        )
                )
                .toList();
    }

    @GetMapping("/turnos")
    public List<TurnoCatalogoResponse> turnos() {
        return catalogoPersonalUseCase
                .turnos()
                .stream()
                .map(item ->
                        new TurnoCatalogoResponse(
                                item.id(),
                                item.codigo(),
                                item.descripcion()
                        )
                )
                .toList();
    }

    @GetMapping("/motivos-ausencia")
    public List<MotivoAusenciaCatalogoResponse> motivos() {
        return motivoAusenciaUseCase
                .listar()
                .stream()
                .map(item ->
                        new MotivoAusenciaCatalogoResponse(
                                item.id(),
                                item.nombre()
                        )
                )
                .toList();
    }
}
