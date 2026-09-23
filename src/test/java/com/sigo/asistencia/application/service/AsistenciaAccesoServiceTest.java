package com.sigo.asistencia.application.service;

import com.sigo.asistencia.application.port.out.AsistenciaUsuarioActualPort;
import org.junit.jupiter.api.Test;
import com.sigo.shared.exception.ForbiddenException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AsistenciaAccesoServiceTest {

    @Test
    void permiteSupervisor() {
        AsistenciaAccesoService service =
                new AsistenciaAccesoService(
                        () -> new AsistenciaUsuarioActualPort.UsuarioActual(
                                1L,
                                "SUPERVISOR",
                                4L
                        )
                );

        assertDoesNotThrow(service::exigirGestion);
    }

    @Test
    void permiteControlador() {
        AsistenciaAccesoService service =
                new AsistenciaAccesoService(
                        () -> new AsistenciaUsuarioActualPort.UsuarioActual(
                                2L,
                                "CONTROLADOR",
                                4L
                        )
                );

        assertDoesNotThrow(service::exigirGestion);
    }

    @Test
    void rechazaOperador() {
        AsistenciaAccesoService service =
                new AsistenciaAccesoService(
                        () -> new AsistenciaUsuarioActualPort.UsuarioActual(
                                3L,
                                "OPERADOR",
                                4L
                        )
                );

        assertThrows(
                ForbiddenException.class,
                service::exigirGestion
        );
    }
}
