package com.sigo.personal.application.port.in;

import java.util.List;

public interface CatalogoPersonalUseCase {

    List<PlazaItem> plazas();

    List<TurnoItem> turnos();

    record PlazaItem(
            Long id,
            String codigo,
            String descripcion,
            Boolean activo
    ) {
    }

    record TurnoItem(
            Long id,
            String codigo,
            String nombre
    ) {
    }
}
