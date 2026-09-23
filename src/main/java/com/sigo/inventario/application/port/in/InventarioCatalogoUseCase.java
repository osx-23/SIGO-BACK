package com.sigo.inventario.application.port.in;

import java.util.List;

public interface InventarioCatalogoUseCase {

    List<Item> categorias();

    List<Item> ambitos();

    List<Item> roles();

    List<Item> plazas();

    record Item(
            Long id,
            String codigo,
            String nombre
    ) {
    }
}
