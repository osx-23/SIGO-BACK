package com.sigo.inventario.application.port.out;

import java.util.Map;

public interface InventarioAuditoriaPort {

    void registrar(
            Long usuarioId,
            String accion,
            String entidad,
            Long entidadId,
            Map<String, Object> detalle
    );
}
