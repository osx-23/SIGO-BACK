package com.sigo.inventario.application.service;

import com.sigo.inventario.application.port.out.InventarioAuditoriaPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventarioAuditoriaService {

    private final InventarioAuditoriaPort auditoriaPort;

    public void registrar(
            Long usuarioId,
            String accion,
            String entidad,
            Long entidadId,
            Map<String, Object> detalle
    ) {
        auditoriaPort.registrar(
                usuarioId,
                accion,
                entidad,
                entidadId,
                detalle
        );
    }
}
