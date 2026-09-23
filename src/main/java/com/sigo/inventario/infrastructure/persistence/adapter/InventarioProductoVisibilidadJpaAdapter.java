package com.sigo.inventario.infrastructure.persistence.adapter;

import com.sigo.inventario.application.port.out.InventarioProductoVisibilidadPort;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventarioProductoVisibilidadJpaAdapter
        implements InventarioProductoVisibilidadPort {

    private final InventarioProductoRepository productoRepository;

    @Override
    public boolean esVisiblePara(
            Long productoId,
            Long rolId,
            Long plazaId
    ) {
        return productoRepository.esVisiblePara(
                productoId,
                rolId,
                plazaId
        );
    }
}
