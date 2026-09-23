package com.sigo.inventario.application.port.in;

import com.sigo.inventario.application.security.InventarioUsuarioActual;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public interface InventarioStockUseCase {

    List<Stock> consultar(
            InventarioUsuarioActual usuario,
            Long plazaId,
            String buscar
    );

    record Stock(
            Long plazaId,
            String plaza,
            Long productoId,
            String producto,
            String unidadMedida,
            BigDecimal cantidadActual,
            Integer stockMinimo,
            boolean bajoMinimo,
            Long inventarioId,
            OffsetDateTime actualizadoEn
    ) {
    }
}
