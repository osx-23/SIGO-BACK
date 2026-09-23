package com.sigo.inventario.application.port.out;

import com.sigo.inventario.application.port.in.InventarioStockUseCase;

import java.util.List;

public interface InventarioStockQueryPort {

    List<InventarioStockUseCase.Stock> buscar(
            Long plazaId,
            String texto
    );
}
