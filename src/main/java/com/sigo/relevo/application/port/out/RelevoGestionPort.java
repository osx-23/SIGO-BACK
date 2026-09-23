package com.sigo.relevo.application.port.out;

import com.sigo.relevo.application.port.in.GestionarRelevoUseCase;

import java.util.List;

public interface RelevoGestionPort {

    List<ElementoConfig> elementosActivos();

    ViaConfig requireVia(Long viaId);

    Long registrar(GestionarRelevoUseCase.Command command);

    Long actualizar(
            Long id,
            GestionarRelevoUseCase.Command command
    );

    record ElementoConfig(
            Long id,
            String nombre,
            Boolean requiereCantidad
    ) {
    }

    record ViaConfig(
            Long id,
            Long plazaId,
            Integer numero,
            Boolean activa
    ) {
    }
}
