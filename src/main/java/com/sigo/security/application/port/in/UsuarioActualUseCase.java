package com.sigo.security.application.port.in;

public interface UsuarioActualUseCase {

    UsuarioActual requireActual();

    record UsuarioActual(
            Long id,
            Integer codigo,
            String nombre,
            String rol,
            Long plazaId,
            String plazaCodigo,
            Long puestoId,
            String puestoNombre
    ) {
    }
}
