package com.sigo.security.application.port.out;

import com.sigo.security.domain.UsuarioSeguridad;

import java.util.Optional;

public interface UsuarioSeguridadPort {

    Optional<UsuarioSeguridad> buscarPorCodigo(Integer codigo);

    Optional<UsuarioSeguridad> buscarPorId(Long id);

    void actualizarPassword(
            Long trabajadorId,
            String passwordHash,
            boolean requiereCambioPassword
    );
}
