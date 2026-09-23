package com.sigo.inventario.application.port.in;

import java.math.BigDecimal;
import java.util.List;

public interface GuardarConteoInventarioUseCase {

    InventarioConsultaUseCase.Detalle guardar(
            Usuario usuario,
            Long inventarioId,
            List<Item> productos
    );

    record Usuario(
            Long trabajadorId,
            Long plazaId,
            Long rolId,
            String rolCodigo
    ) {
    }

    record Item(
            Long productoId,
            BigDecimal cantidad
    ) {
    }
}
