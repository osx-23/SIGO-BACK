package com.sigo.programacion.infrastructure.security;

import com.sigo.personal.infrastructure.persistence.entity.RolSistema;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.programacion.application.port.out.SupervisorActualPort;
import com.sigo.security.application.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class SupervisorActualAdapter
        implements SupervisorActualPort {

    private final CurrentUserService currentUserService;

    @Override
    public Long requireSupervisorId() {
        Trabajador trabajador =
                currentUserService.requireCurrent();

        if (trabajador.getRolSistema()
                != RolSistema.SUPERVISOR) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo el Supervisor puede realizar esta operación"
            );
        }

        return trabajador.getId();
    }
}
