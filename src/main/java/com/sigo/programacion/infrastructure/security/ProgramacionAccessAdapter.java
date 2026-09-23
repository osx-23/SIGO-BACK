package com.sigo.programacion.infrastructure.security;

import com.sigo.personal.infrastructure.persistence.entity.RolSistema;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.programacion.application.port.out.ProgramacionAccessPort;
import com.sigo.security.application.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ProgramacionAccessAdapter
        implements ProgramacionAccessPort {

    private final CurrentUserService currentUserService;

    @Override
    public Long requireSupervisorId() {
        Trabajador actual = currentUserService.requireCurrent();

        if (actual.getRolSistema() != RolSistema.SUPERVISOR) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo el Supervisor puede realizar esta operación"
            );
        }

        return actual.getId();
    }

    @Override
    public void validarLecturaPlaza(Long plazaId) {
        Trabajador actual = currentUserService.requireCurrent();

        if (actual.getRolSistema() == RolSistema.SUPERVISOR) {
            return;
        }

        if (actual.getRolSistema() != RolSistema.CONTROLADOR) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tienes acceso a la programación mensual"
            );
        }

        if (actual.getPlaza() == null
                || !Objects.equals(actual.getPlaza().getId(), plazaId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo puedes consultar tu propia plaza"
            );
        }
    }

    @Override
    public void validarGestionPlaza(Long plazaId) {
        Trabajador actual = currentUserService.requireCurrent();

        if (actual.getRolSistema() == RolSistema.SUPERVISOR) {
            return;
        }

        if (actual.getRolSistema() != RolSistema.CONTROLADOR) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo Supervisor o Controlador puede gestionar la distribución"
            );
        }

        if (actual.getPlaza() == null
                || !Objects.equals(actual.getPlaza().getId(), plazaId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El controlador solo puede gestionar su propia plaza"
            );
        }
    }
    @Override
    public Long requireGestionPlazaUsuarioId(Long plazaId) {
        validarGestionPlaza(plazaId);
        return currentUserService.requireCurrent().getId();
    }

}
