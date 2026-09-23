package com.sigo.programacion.application.port.in;

import java.util.List;

public interface UbicacionUseCase {

    List<Ubicacion> listarActivas(Long plazaId);

    List<Ubicacion> listarConfiguracion(Long plazaId);

    Ubicacion crear(Command command);

    Ubicacion actualizar(Long ubicacionId, Command command);

    Ubicacion cambiarEstado(Long ubicacionId, boolean activo);

    record Command(
            Long plazaId,
            String codigo,
            String nombre,
            String tipo,
            Integer orden
    ) {
    }

    record Ubicacion(
            Long id,
            Long plazaId,
            String codigo,
            String nombre,
            String tipo,
            Long viaId,
            Boolean activo,
            Integer orden
    ) {
    }
}
