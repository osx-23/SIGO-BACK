package com.sigo.asistencia.infrastructure.security;

import com.sigo.asistencia.application.port.out.AsistenciaUsuarioActualPort;
import com.sigo.security.application.port.in.UsuarioActualUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AsistenciaUsuarioActualAdapter
        implements AsistenciaUsuarioActualPort {

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
