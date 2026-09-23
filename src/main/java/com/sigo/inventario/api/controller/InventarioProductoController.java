package com.sigo.inventario.api.controller;

import com.sigo.inventario.api.dto.request.ProductoGuardarRequest;
import com.sigo.inventario.api.dto.response.ProductoAdminResponse;
import com.sigo.inventario.application.port.in.InventarioProductoUseCase;
import com.sigo.inventario.application.security.InventarioUsuarioActual;
import com.sigo.inventario.application.security.InventarioUsuarioContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/productos")
@RequiredArgsConstructor
public class InventarioProductoController {

    private final InventarioProductoUseCase useCase;
    private final InventarioUsuarioContextService usuarios;

    @GetMapping
    public List<ProductoAdminResponse> listar() {
        return useCase
                .listar(usuario())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    public ProductoAdminResponse crear(
            @Valid @RequestBody ProductoGuardarRequest request
    ) {
        return toResponse(
                useCase.crear(
                        usuario(),
                        toCommand(request)
                )
        );
    }

    @PutMapping("/{id}")
    public ProductoAdminResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProductoGuardarRequest request
    ) {
        return toResponse(
                useCase.actualizar(
                        usuario(),
                        id,
                        toCommand(request)
                )
        );
    }

    @PatchMapping("/{id}/estado")
    public ProductoAdminResponse estado(
            @PathVariable Long id,
            @RequestParam boolean activo
    ) {
        return toResponse(
                useCase.cambiarEstado(
                        usuario(),
                        id,
                        activo
                )
        );
    }

    private InventarioProductoUseCase.Usuario usuario() {
        InventarioUsuarioActual actual =
                usuarios.obtenerActual();

        return new InventarioProductoUseCase.Usuario(
                actual.trabajadorId(),
                actual.plazaId(),
                actual.rolCodigo()
        );
    }

    private InventarioProductoUseCase.Command toCommand(
            ProductoGuardarRequest request
    ) {
        return new InventarioProductoUseCase.Command(
                request.codigo(),
                request.nombre(),
                request.descripcion(),
                request.categoriaId(),
                request.ambitoId(),
                request.unidadMedida(),
                request.activo(),
                request.roles(),
                request.plazas()
                        .stream()
                        .map(plaza ->
                                new InventarioProductoUseCase.PlazaConfig(
                                        plaza.plazaId(),
                                        plaza.stockMinimo()
                                )
                        )
                        .toList()
        );
    }

    private ProductoAdminResponse toResponse(
            InventarioProductoUseCase.Producto producto
    ) {
        return new ProductoAdminResponse(
                producto.id(),
                producto.codigo(),
                producto.nombre(),
                producto.descripcion(),
                producto.categoriaId(),
                producto.categoria(),
                producto.ambitoId(),
                producto.ambito(),
                producto.unidadMedida(),
                producto.activo(),
                producto.roles(),
                producto.plazas()
                        .stream()
                        .map(plaza ->
                                new ProductoAdminResponse.ProductoPlazaConfigResponse(
                                        plaza.plazaId(),
                                        plaza.plaza(),
                                        plaza.stockMinimo()
                                )
                        )
                        .toList()
        );
    }
}
