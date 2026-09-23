package com.sigo.programacion.application.port.out;

import com.sigo.programacion.application.port.in.ListarSecuenciasUseCase;

import java.util.List;

public interface SecuenciaGestionPort {

    List<ListarSecuenciasUseCase.Secuencia> listar(Long plazaId);

    ListarSecuenciasUseCase.Secuencia asignar(
            Long agenteId,
            Long plazaId,
            String grupo,
            Long actualizadoPorId
    );
}
