package com.sigo.relevo.infrastructure.security;

import com.sigo.relevo.application.port.out.RelevoUsuarioActualPort;
import com.sigo.security.application.port.in.UsuarioActualUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RelevoUsuarioActualAdapter
        implements RelevoUsuarioActualPort {

    private final UsuarioActualUseCase usuarioActualUseCase;

    @Override
    public UsuarioActual requireActual() {
        UsuarioActualUseCase.UsuarioActual actual =
                usuarioActualUseCase.requireActual();

        return new UsuarioActual(
                actual.id(),
                actual.rol(),
                actual.plazaId()
        );
    }
}
