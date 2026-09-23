package com.sigo.inventario.application.port.out;

import com.sigo.inventario.application.port.in.InventarioCatalogoUseCase;

import java.util.List;

public interface InventarioCatalogoQueryPort {

    List<InventarioCatalogoUseCase.Item> categorias();

    List<InventarioCatalogoUseCase.Item> ambitos();

    List<InventarioCatalogoUseCase.Item> roles();

    List<InventarioCatalogoUseCase.Item> plazas();
}
