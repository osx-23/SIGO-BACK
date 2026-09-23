package com.sigo.inventario.application.port.out;

import com.sigo.inventario.application.security.InventarioUsuarioActual;

public interface InventarioUsuarioContextPort {

    InventarioUsuarioActual obtenerActual();
}
