package com.sigo.relevo.application.service;

import com.sigo.relevo.application.port.in.GestionarRelevoUseCase;
import com.sigo.relevo.application.port.in.RelevoAccesoUseCase;
import com.sigo.relevo.application.port.in.RelevoHistorialUseCase;
import com.sigo.relevo.application.port.out.RelevoUsuarioActualPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RelevoAccesoService
        implements RelevoAccesoUseCase {

    private final RelevoUsuarioActualPort usuarioActualPort;

    @Override
    public GestionarRelevoUseCase.Command prepararRegistro(
            GestionarRelevoUseCase.Command command
    ) {
        RelevoUsuarioActualPort.UsuarioActual actual =
                usuarioActualPort.requireActual();

        if (!"OPERADOR".equals(actual.rol())) {
            return command;
        }

        if (actual.plazaId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El operador no tiene una plaza asignada"
            );
        }

        return new GestionarRelevoUseCase.Command(
                actual.plazaId(),
                command.turnoId(),
                actual.id(),
                command.fecha(),
                command.hora(),
                command.observaciones(),
                command.resumen(),
                command.checklist(),
                command.vias()
        );
    }

    @Override
    public void exigirPuedeActualizar() {
        String rol = usuarioActualPort
                .requireActual()
                .rol();

        if ("OPERADOR".equals(rol)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Los operadores solo pueden consultar el historial de relevos"
            );
        }
    }

    @Override
    public RelevoHistorialUseCase.Usuario usuarioActual() {
        RelevoUsuarioActualPort.UsuarioActual actual =
                usuarioActualPort.requireActual();

        return new RelevoHistorialUseCase.Usuario(
                actual.rol(),
                actual.plazaId()
        );
    }
}
