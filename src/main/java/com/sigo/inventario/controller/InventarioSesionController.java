package com.sigo.inventario.controller;

import com.sigo.inventario.dto.response.InventarioUsuarioResponse;
import com.sigo.inventario.security.InventarioUsuarioActual;
import com.sigo.inventario.security.InventarioUsuarioContextService;
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
                usuario.trabajador().getId(),
                usuario.trabajador().getCodigo(),
                usuario.trabajador().getNombreCompleto(),
                usuario.rolCodigo(),
                usuario.trabajador().getPlaza() != null
                        ? usuario.trabajador().getPlaza().getId()
                        : null,
                usuario.trabajador().getPlaza() != null
                        ? usuario.trabajador().getPlaza().getCodigo()
                        : null
        );
    }
}