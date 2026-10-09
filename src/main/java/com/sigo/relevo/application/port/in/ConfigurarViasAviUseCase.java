package com.sigo.relevo.application.port.in;

import java.util.List;

public interface ConfigurarViasAviUseCase {

    List<ViaConfig> listar(Long plazaId);

    void actualizar(
            Long plazaId,
            Long viaId,
            boolean visible
    );

    record ViaConfig(
            Long id,
            Long plazaId,
            Integer numero,
            String nombre,
            Boolean activa,
            Integer orden,
            Boolean aviVisible
    ) {
    }
}
