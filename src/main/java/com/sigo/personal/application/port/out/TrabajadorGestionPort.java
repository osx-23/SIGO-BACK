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

    List<TrabajadorUseCase.PuestoData> listarPuestosAdministrables();

    boolean existeCodigo(Integer codigo);

    TrabajadorUseCase.TrabajadorData crearUsuario(
            Integer codigo,
            String nombreCompleto,
            Long puestoId,
            Long plazaId,
            String passwordInicial
    );

    PreparacionActualizacion prepararActualizacion(
            Long trabajadorId,
            Long nuevaPlazaId,
            Long nuevoPuestoId,
            Boolean activo
    );

    TrabajadorUseCase.TrabajadorData aplicarActualizacion(
            Long trabajadorId,
            Long nuevaPlazaId,
            Long nuevoPuestoId,
            Boolean activo
    );

    record PreparacionActualizacion(
            Long trabajadorId,
            Long plazaAnteriorId,
            Long nuevaPlazaId,
            boolean cambioPlaza,
            boolean cambioPuesto,
            boolean quedaraInactivo,
            boolean nuevoEsOperador
    ) {
    }
}
