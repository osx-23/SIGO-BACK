package com.sigo.programacion.infrastructure.security;

import com.sigo.programacion.application.port.out.ProgramacionAccessPort;
import com.sigo.security.application.port.in.UsuarioActualUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ProgramacionAccessAdapter
        implements ProgramacionAccessPort {

    private final UsuarioActualUseCase usuarioActualUseCase;

    @Override
    public Long requireSupervisorId() {
        UsuarioActualUseCase.UsuarioActual actual =
                usuarioActualUseCase.requireActual();

        if (!"SUPERVISOR".equals(actual.rol())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo el Supervisor puede realizar esta operación"
            );
        }

        return actual.id();
    }

    @Override
    public void validarLecturaPlaza(Long plazaId) {
        UsuarioActualUseCase.UsuarioActual actual =
                usuarioActualUseCase.requireActual();

        if ("SUPERVISOR".equals(actual.rol())) {
            return;
        }

        if (!"CONTROLADOR".equals(actual.rol())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tienes acceso a la programación mensual"
            );
        }

        if (!Objects.equals(actual.plazaId(), plazaId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo puedes consultar tu propia plaza"
            );
        }
    }

    @Override
    public void validarGestionPlaza(Long plazaId) {
        UsuarioActualUseCase.UsuarioActual actual =
                usuarioActualUseCase.requireActual();

        if ("SUPERVISOR".equals(actual.rol())) {
            return;
        }

        if (!"CONTROLADOR".equals(actual.rol())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo Supervisor o Controlador puede gestionar la distribución"
            );
        }

        if (!Objects.equals(actual.plazaId(), plazaId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El controlador solo puede gestionar su propia plaza"
            );
        }
    }

    @Override
    public Long requireGestionPlazaUsuarioId(Long plazaId) {
        validarGestionPlaza(plazaId);
        return usuarioActualUseCase.requireActual().id();
    }

    @Override
    public Long currentUserId() {
        return usuarioActualUseCase.requireActual().id();
    }
}
