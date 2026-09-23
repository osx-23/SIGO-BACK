package com.sigo.security.application.port.out;

import com.sigo.security.application.port.in.UsuarioActualUseCase;

import java.util.Optional;

public interface UsuarioActualDataPort {

    Optional<UsuarioActualUseCase.UsuarioActual>
    buscarActivoPorCodigo(Integer codigo);
}
