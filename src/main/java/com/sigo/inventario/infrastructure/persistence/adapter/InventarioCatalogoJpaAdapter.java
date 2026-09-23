package com.sigo.inventario.infrastructure.persistence.adapter;

import com.sigo.inventario.application.port.in.InventarioCatalogoUseCase;
import com.sigo.inventario.application.port.out.InventarioCatalogoQueryPort;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioAmbitoRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioCategoriaRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioRolRepository;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InventarioCatalogoJpaAdapter
        implements InventarioCatalogoQueryPort {

    private final InventarioCategoriaRepository categorias;
    private final InventarioAmbitoRepository ambitos;
    private final InventarioRolRepository roles;
    private final PlazaRepository plazas;

    @Override
    public List<InventarioCatalogoUseCase.Item> categorias() {
        return categorias
                .findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(item ->
                        new InventarioCatalogoUseCase.Item(
                                item.getId(),
                                null,
                                item.getNombre()
                        )
                )
                .toList();
    }

    @Override
    public List<InventarioCatalogoUseCase.Item> ambitos() {
        return ambitos
                .findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(item ->
                        new InventarioCatalogoUseCase.Item(
                                item.getId(),
                                item.getCodigo(),
                                item.getNombre()
                        )
                )
                .toList();
    }

    @Override
    public List<InventarioCatalogoUseCase.Item> roles() {
        return roles
                .findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(item ->
                        new InventarioCatalogoUseCase.Item(
                                item.getId(),
                                item.getCodigo(),
                                item.getNombre()
                        )
                )
                .toList();
    }

    @Override
    public List<InventarioCatalogoUseCase.Item> plazas() {
        return plazas
                .findByActivoTrueOrderByCodigoAsc()
                .stream()
                .map(item ->
                        new InventarioCatalogoUseCase.Item(
                                item.getId(),
                                item.getCodigo(),
                                item.getDescripcion()
                        )
                )
                .toList();
    }
}
