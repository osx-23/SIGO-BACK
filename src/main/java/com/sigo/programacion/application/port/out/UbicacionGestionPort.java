package com.sigo.programacion.application.port.out;

import com.sigo.programacion.application.port.in.UbicacionUseCase;

import java.util.List;

public interface UbicacionGestionPort {

    List<UbicacionUseCase.Ubicacion> listarActivas(Long plazaId);

    List<UbicacionUseCase.Ubicacion> listarTodas(Long plazaId);

    UbicacionUseCase.Ubicacion crear(
            Long plazaId,
            String codigo,
            String nombre,
            String tipo,
            Integer orden
    );

    UbicacionUseCase.Ubicacion actualizar(
            Long ubicacionId,
            Long plazaId,
            String codigo,
            String nombre,
            String tipo,
            Integer orden
    );

    UbicacionUseCase.Ubicacion cambiarEstado(
            Long ubicacionId,
            boolean activo
    );
}
