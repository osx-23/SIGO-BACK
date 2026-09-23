package com.sigo.programacion.application.port.out;

import com.sigo.programacion.application.port.in.GuardarOrdenSecuenciaUseCase;

import java.util.List;

public interface SecuenciaOrdenPort {

    boolean existePlazaActiva(Long plazaId);

    void eliminarSecuenciasInactivas(Long plazaId);

    List<Long> obtenerAgentesActivos(
            Long plazaId,
            String grupo
    );

    void reordenar(
            Long plazaId,
            String grupo,
            List<GuardarOrdenSecuenciaUseCase.Item> agentes,
            Long actualizadoPorId
    );
}
