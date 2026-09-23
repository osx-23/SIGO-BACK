package com.sigo.relevo.application.port.out;

public interface RelevoUsuarioActualPort {

    UsuarioActual requireActual();

    record UsuarioActual(
            Long id,
            String rol,
            Long plazaId
    ) {
    }
}
