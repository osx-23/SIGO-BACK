package com.sigo.asistencia.application.port.in;

import java.util.List;

public interface MotivoAusenciaCatalogoUseCase {

    List<MotivoItem> listar();

    record MotivoItem(
            Long id,
            String nombre
    ) {
    }
}
