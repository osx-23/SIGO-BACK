package com.sigo.personal.application.port.in;

import java.util.List;

public interface TrabajadorUseCase {

    List<TrabajadorData> listarAgentesPorPlaza(Long plazaId);

    List<TrabajadorData> listarControladoresPorPlaza(Long plazaId);

    List<TrabajadorData> listarAdministracion(Long plazaId);

    List<PuestoData> listarPuestosAdministrables();

    TrabajadorData crearUsuario(
            Integer codigo,
            String nombreCompleto,
            Long puestoId,
            Long plazaId,
            String passwordInicial
    );

    TrabajadorData actualizarAdministracion(
            Long trabajadorId,
            Long plazaId,
            Long puestoId,
            Boolean activo
    );

    record PuestoData(
            Long id,
            String nombre
    ) {
    }

    record PlazaData(
            Long id,
            String codigo,
            String descripcion,
            Boolean activo
    ) {
    }

    record TrabajadorData(
            Long id,
            Integer codigo,
            String nombreCompleto,
            PuestoData puesto,
            PlazaData plaza,
            String rolSistema,
            Boolean requiereCambioPassword,
            Boolean activo
    ) {
    }
}
