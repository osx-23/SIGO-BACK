package com.sigo.inventario.application.port.in;

import java.math.BigDecimal;
import java.util.List;

public interface CorregirInventarioUseCase {
    InventarioConsultaUseCase.Detalle corregir(
            Usuario usuario, Long inventarioId, List<Item> productos, String motivo);

    record Usuario(Long trabajadorId, Long plazaId, String rolSistema, String rolInventario) {}
    record Item(Long productoId, BigDecimal cantidad) {}
}
