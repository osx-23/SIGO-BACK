package com.sigo.asistencia.application.port.out;

public interface AsistenciaUsuarioActualPort {

    UsuarioActual requireActual();

    record UsuarioActual(
            Long id,
            String rol,
            Long plazaId
    ) {
    }
}
