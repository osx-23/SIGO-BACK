package com.sigo.inventario.api.controller;

import com.sigo.inventario.api.dto.response.InventarioUsuarioResponse;
import com.sigo.inventario.application.security.InventarioUsuarioActual;
import com.sigo.inventario.application.security.InventarioUsuarioContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
public class InventarioSesionController {

    private final InventarioUsuarioContextService usuarios;

    @GetMapping("/me")
    public InventarioUsuarioResponse me(
            ) {

        InventarioUsuarioActual usuario = usuarios.obtenerActual();

        return new InventarioUsuarioResponse(
                usuario.trabajadorId(),
                usuario.codigo(),
                usuario.nombre(),
                usuario.rolCodigo(),
                usuario.plazaId(),
                usuario.plazaCodigo()
        );
    }
}