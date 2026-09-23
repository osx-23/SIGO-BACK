package com.sigo.inventario.application.port.in;

import com.sigo.inventario.application.security.InventarioUsuarioActual;

public interface ObtenerInventarioUsuarioUseCase {

    InventarioUsuarioActual obtenerActual();
}
