package com.sigo.inventario.application.port.out;

public interface InventarioCierrePort {

    void finalizar(Long inventarioId);

    void anular(
            Long inventarioId,
            Long usuarioId,
            String motivo
    );
}
