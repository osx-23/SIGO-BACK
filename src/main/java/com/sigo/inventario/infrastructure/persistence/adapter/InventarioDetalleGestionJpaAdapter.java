package com.sigo.inventario.infrastructure.persistence.adapter;

import com.sigo.inventario.application.port.in.GuardarConteoInventarioUseCase;
import com.sigo.inventario.application.port.out.InventarioDetalleGestionPort;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioConteo;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioConteoDetalle;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioProducto;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioConteoDetalleRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioConteoRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioProductoRepository;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InventarioDetalleGestionJpaAdapter
        implements InventarioDetalleGestionPort {

    private final InventarioConteoRepository conteoRepository;
    private final InventarioConteoDetalleRepository detalleRepository;
    private final InventarioProductoRepository productoRepository;

    @Override
    public void guardar(
            Long inventarioId,
            List<GuardarConteoInventarioUseCase.Item> productos
    ) {
        InventarioConteo inventario = conteoRepository
                .findConDetalleById(inventarioId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inventario no encontrado"
                        )
                );

        for (GuardarConteoInventarioUseCase.Item item
                : productos) {
            InventarioProducto producto = productoRepository
                    .findById(item.productoId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Producto no encontrado"
                            )
                    );

            InventarioConteoDetalle detalle =
                    detalleRepository
                            .findByInventarioIdAndProductoId(
                                    inventarioId,
                                    item.productoId()
                            )
                            .orElseGet(
                                    InventarioConteoDetalle::new
                            );

            detalle.setInventario(inventario);
            detalle.setProducto(producto);
            detalle.setCantidadEncontrada(
                    item.cantidad()
            );
            detalle.setNombreProductoSnapshot(
                    producto.getNombre()
            );
            detalle.setUnidadSnapshot(
                    producto.getUnidadMedida()
            );
            detalle.setCategoriaSnapshot(
                    producto.getCategoria() == null
                            ? null
                            : producto
                                    .getCategoria()
                                    .getNombre()
            );
            detalle.setAmbitoSnapshot(
                    producto.getAmbito() == null
                            ? null
                            : producto
                                    .getAmbito()
                                    .getNombre()
            );

            detalleRepository.save(detalle);
        }
    }
}
