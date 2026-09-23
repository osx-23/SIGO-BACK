package com.sigo.inventario.application.port.in;

import com.sigo.inventario.domain.InventarioEstado;

import java.time.OffsetDateTime;

public interface IniciarInventarioUseCase {

    Resumen iniciar(Usuario usuario);

    record Usuario(
            Long trabajadorId,
            Long plazaId,
            Long rolId,
            String rolCodigo
    ) {
    }

    record Resumen(
            Long id,
            Long plazaId,
            String plaza,
            Long responsableId,
            Integer codigoResponsable,
            String responsable,
            String rol,
            OffsetDateTime fechaInicio,
            OffsetDateTime fechaFinalizacion,
            InventarioEstado estado,
            long productosRegistrados
    ) {
    }
}
