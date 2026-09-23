package com.sigo.security.application.service;

import com.sigo.security.application.port.in.UsuarioActualUseCase;
import com.sigo.security.application.port.out.UsuarioActualDataPort;
import com.sigo.security.domain.AutenticacionException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UsuarioActualServiceTest {

    @Test
    void obtieneUsuarioActivoPorCodigoAutenticado() {
        UsuarioActualService service =
                new UsuarioActualService(
                        () -> 287,
                        codigo -> Optional.of(
                                new UsuarioActualUseCase.UsuarioActual(
                                        10L,
                                        codigo,
                                        "Controlador Test",
                                        "CONTROLADOR",
                                        4L,
                                        "P4",
                                        2L,
                                        "Controlador"
                                )
                        )
                );

        var actual = service.requireActual();

        assertEquals(287, actual.codigo());
        assertEquals("CONTROLADOR", actual.rol());
        assertEquals(4L, actual.plazaId());
    }

    @Test
    void rechazaUsuarioNoEncontrado() {
        UsuarioActualDataPort dataPort =
                codigo -> Optional.empty();

        UsuarioActualService service =
                new UsuarioActualService(
                        () -> 9999,
                        dataPort
                );

        assertThrows(
                AutenticacionException.class,
                service::requireActual
        );
    }
}
