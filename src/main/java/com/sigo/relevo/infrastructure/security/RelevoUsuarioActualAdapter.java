package com.sigo.relevo.infrastructure.security;

import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.relevo.application.port.out.RelevoUsuarioActualPort;
import com.sigo.security.application.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RelevoUsuarioActualAdapter
        implements RelevoUsuarioActualPort {

    private final CurrentUserService currentUserService;

    @Override
    public UsuarioActual requireActual() {
        Trabajador actual =
                currentUserService.requireCurrent();

        return new UsuarioActual(
                actual.getId(),
                actual.getRolSistema().name(),
                actual.getPlaza() == null
                        ? null
                        : actual.getPlaza().getId()
        );
    }
}
