package com.sigo.inventario.application.port.out;

import com.sigo.inventario.application.port.in.CorregirInventarioUseCase;
import java.math.BigDecimal;
import java.util.List;

public interface InventarioCorreccionPort {
    List<Cambio> corregir(Long inventarioId, List<CorregirInventarioUseCase.Item> productos);

    record Cambio(Long productoId, BigDecimal anterior, BigDecimal nuevo) {}
}
