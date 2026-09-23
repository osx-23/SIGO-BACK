package com.sigo.asistencia.application.service;

import com.sigo.asistencia.application.port.in.AsistenciaAccesoUseCase;
import com.sigo.asistencia.application.port.out.AsistenciaUsuarioActualPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AsistenciaAccesoService
        implements AsistenciaAccesoUseCase {

    private final AsistenciaUsuarioActualPort usuarioActualPort;

    @Override
    public void exigirGestion() {
        String rol = usuarioActualPort
                .requireActual()
                .rol();

        if (!"SUPERVISOR".equals(rol)
                && !"CONTROLADOR".equals(rol)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo supervisores y controladores pueden gestionar asistencia"
            );
        }
    }
}
