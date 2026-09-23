package com.sigo.inventario.application.port.out;

import com.sigo.inventario.application.port.in.InventarioConsultaUseCase;
import com.sigo.inventario.domain.InventarioEstado;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface InventarioConsultaPort {

    Cabecera requireInventario(Long inventarioId);

    List<InventarioConsultaUseCase.Producto> productosPermitidos(
            Long rolId,
            Long plazaId
    );

    List<InventarioConsultaUseCase.DetalleItem> detalleItems(
            Long inventarioId
    );

    Optional<Long> rolActivoId(String codigo);

    InventarioConsultaUseCase.Pagina<InventarioConsultaUseCase.Resumen> historial(
            FiltroHistorial filtro
    );

    record Cabecera(
            Long id,
            Long plazaId,
            String plazaCodigo,
            Long responsableId,
            Integer responsableCodigo,
            String responsableNombre,
            Long rolId,
            String rolCodigo,
            OffsetDateTime fechaInicio,
            OffsetDateTime fechaFinalizacion,
            InventarioEstado estado,
            String observacion,
            String motivoAnulacion
    ) {
    }

    record FiltroHistorial(
            Long plazaId,
            Long responsableId,
            Long rolId,
            InventarioEstado estado,
            OffsetDateTime fechaDesde,
            OffsetDateTime fechaHasta,
            int pagina,
            int tamanio
    ) {
    }
}
