package com.sigo.inventario.application.service;

import com.sigo.inventario.application.port.in.InventarioCatalogoUseCase;
import com.sigo.inventario.application.port.out.InventarioCatalogoQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventarioCatalogoService
        implements InventarioCatalogoUseCase {

    private final InventarioCatalogoQueryPort queryPort;

    @Override
    @Transactional(readOnly = true)
    public List<Item> categorias() {
        return queryPort.categorias();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Item> ambitos() {
        return queryPort.ambitos();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Item> roles() {
        return queryPort.roles();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Item> plazas() {
        return queryPort.plazas();
    }
}
