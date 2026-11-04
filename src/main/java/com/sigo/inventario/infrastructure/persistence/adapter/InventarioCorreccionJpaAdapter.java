package com.sigo.inventario.infrastructure.persistence.adapter;

import com.sigo.inventario.application.port.in.CorregirInventarioUseCase;
import com.sigo.inventario.application.port.out.InventarioCorreccionPort;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioConteoDetalleRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioConteoRepository;
import com.sigo.shared.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InventarioCorreccionJpaAdapter implements InventarioCorreccionPort {
    private final InventarioConteoRepository conteos;
    private final InventarioConteoDetalleRepository detalles;

    @Override
    public List<Cambio> corregir(Long inventarioId, List<CorregirInventarioUseCase.Item> productos) {
        // Serializa correcciones del mismo inventario y verifica el estado dentro del bloqueo.
        if (conteos.bloquearFinalizado(inventarioId).isEmpty()) {
            throw new ConflictException("El inventario ya no está FINALIZADO");
        }

        var registrados = detalles.findByInventarioIdOrderByNombreProductoSnapshotAsc(inventarioId);
        Map<Long, CorregirInventarioUseCase.Item> enviados = productos.stream()
                .collect(Collectors.toMap(CorregirInventarioUseCase.Item::productoId, Function.identity()));

        // Solo permite corregir cantidades existentes: no agrega ni elimina líneas históricas.
        if (registrados.size() != enviados.size()
                || registrados.stream().anyMatch(d -> !enviados.containsKey(d.getProducto().getId()))) {
            throw new ConflictException("Los productos del inventario cambiaron; vuelva a abrir la edición");
        }

        List<Cambio> cambios = new ArrayList<>();
        for (var detalle : registrados) {
            var nuevo = enviados.get(detalle.getProducto().getId()).cantidad();
            var anterior = detalle.getCantidadEncontrada();
            if (anterior.compareTo(nuevo) != 0) {
                cambios.add(new Cambio(detalle.getProducto().getId(), anterior, nuevo));
                detalle.setCantidadEncontrada(nuevo);
            }
        }
        detalles.saveAll(registrados);
        return cambios;
    }
}
