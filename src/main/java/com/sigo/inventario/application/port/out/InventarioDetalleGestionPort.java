package com.sigo.inventario.application.port.out;

import com.sigo.inventario.application.port.in.GuardarConteoInventarioUseCase;

import java.util.List;

public interface InventarioDetalleGestionPort {

    void guardar(
            Long inventarioId,
            List<GuardarConteoInventarioUseCase.Item> productos
    );
}
