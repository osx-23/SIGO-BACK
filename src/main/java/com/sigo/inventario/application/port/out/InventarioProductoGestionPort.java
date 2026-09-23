package com.sigo.inventario.application.port.out;

import com.sigo.inventario.application.port.in.InventarioProductoUseCase;

import java.util.List;

public interface InventarioProductoGestionPort {

    List<InventarioProductoUseCase.Producto> listar(
            Long plazaId
    );

    InventarioProductoUseCase.Producto crear(
            InventarioProductoUseCase.Usuario usuario,
            InventarioProductoUseCase.Command command
    );

    InventarioProductoUseCase.Producto actualizar(
            InventarioProductoUseCase.Usuario usuario,
            Long productoId,
            InventarioProductoUseCase.Command command
    );

    InventarioProductoUseCase.Producto cambiarEstado(
            InventarioProductoUseCase.Usuario usuario,
            Long productoId,
            boolean activo
    );
}
