package com.sigo.personal.application.port.out;

import com.sigo.personal.application.port.in.TrabajadorUseCase;

import java.util.List;

public interface TrabajadorGestionPort {

    List<TrabajadorUseCase.TrabajadorData> listarAgentesPorPlaza(
            Long plazaId
    );

    List<TrabajadorUseCase.TrabajadorData> listarControladoresPorPlaza(
            Long plazaId
    );

    List<TrabajadorUseCase.TrabajadorData> listarAdministracion(
            Long plazaId
    );

    TrabajadorUseCase.TrabajadorData actualizarAdministracion(
            Long trabajadorId,
            Long plazaId,
            Boolean activo
    );
}
