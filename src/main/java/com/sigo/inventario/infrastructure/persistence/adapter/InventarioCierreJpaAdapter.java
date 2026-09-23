package com.sigo.inventario.infrastructure.persistence.adapter;

import com.sigo.inventario.application.port.out.InventarioCierrePort;
import com.sigo.inventario.infrastructure.persistence.entity.EstadoInventario;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioConteo;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioConteoRepository;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class InventarioCierreJpaAdapter
        implements InventarioCierrePort {

    private final InventarioConteoRepository conteoRepository;
    private final TrabajadorRepository trabajadorRepository;

    @Override
    public void finalizar(Long inventarioId) {
        InventarioConteo inventario =
                requireInventario(inventarioId);

        inventario.setEstado(
                EstadoInventario.FINALIZADO
        );

        inventario.setFechaFinalizacion(
                OffsetDateTime.now()
        );

        conteoRepository.save(inventario);
    }

    @Override
    public void anular(
            Long inventarioId,
            Long usuarioId,
            String motivo
    ) {
        InventarioConteo inventario =
                requireInventario(inventarioId);

        Trabajador usuario = trabajadorRepository
                .findById(usuarioId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Usuario no encontrado"
                        )
                );

        inventario.setEstado(
                EstadoInventario.ANULADO
        );
        inventario.setAnuladoPor(usuario);
        inventario.setAnuladoEn(
                OffsetDateTime.now()
        );
        inventario.setMotivoAnulacion(motivo);

        conteoRepository.save(inventario);
    }

    private InventarioConteo requireInventario(
            Long inventarioId
    ) {
        return conteoRepository
                .findConDetalleById(inventarioId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Inventario no encontrado"
                        )
                );
    }
}
