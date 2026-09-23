package com.sigo.inventario.application.port.out;

public interface InventarioProductoVisibilidadPort {

    boolean esVisiblePara(
            Long productoId,
            Long rolId,
            Long plazaId
    );
}
