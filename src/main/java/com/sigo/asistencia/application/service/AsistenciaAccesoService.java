package com.sigo.asistencia.application.service;

import com.sigo.asistencia.application.port.in.AsistenciaAccesoUseCase;
import com.sigo.asistencia.application.port.out.AsistenciaUsuarioActualPort;
import com.sigo.shared.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
            throw new ForbiddenException(
                    "Solo supervisores y controladores pueden gestionar asistencia"
            );
        }
    }
}
