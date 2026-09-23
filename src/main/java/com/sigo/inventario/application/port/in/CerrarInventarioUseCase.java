package com.sigo.inventario.application.port.in;

public interface CerrarInventarioUseCase {

    InventarioConsultaUseCase.Detalle finalizar(
            Usuario usuario,
            Long inventarioId
    );

    InventarioConsultaUseCase.Detalle anular(
            Usuario usuario,
            Long inventarioId,
            String motivo
    );

    record Usuario(
            Long trabajadorId,
            Long plazaId,
            String rolCodigo
    ) {
        public boolean esSupervisor() {
            return "SUPERVISOR".equalsIgnoreCase(rolCodigo);
        }
    }
}
