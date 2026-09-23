package com.sigo.inventario.application.security;

import com.sigo.inventario.application.port.in.ObtenerInventarioUsuarioUseCase;
import com.sigo.inventario.application.port.out.InventarioUsuarioContextPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventarioUsuarioContextService
        implements ObtenerInventarioUsuarioUseCase {

    private final InventarioUsuarioContextPort contextPort;

    @Override
    public InventarioUsuarioActual obtenerActual() {
        return contextPort.obtenerActual();
    }
}
