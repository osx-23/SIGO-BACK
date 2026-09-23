package com.sigo.relevo.application.service;

import com.sigo.relevo.application.port.in.GestionarRelevoUseCase;
import com.sigo.relevo.application.port.out.RelevoUsuarioActualPort;
import org.junit.jupiter.api.Test;
import com.sigo.shared.exception.ForbiddenException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RelevoAccesoServiceTest {

    @Test
    void operadorUsaSuPlazaYSuId() {
        RelevoAccesoService service =
                new RelevoAccesoService(
                        () -> new RelevoUsuarioActualPort.UsuarioActual(
                                55L,
                                "OPERADOR",
                                4L
                        )
                );

        GestionarRelevoUseCase.Command preparado =
                service.prepararRegistro(command());

        assertEquals(4L, preparado.plazaId());
        assertEquals(55L, preparado.operadorId());
    }

    @Test
    void supervisorConservaSolicitudOriginal() {
        RelevoAccesoService service =
                new RelevoAccesoService(
                        () -> new RelevoUsuarioActualPort.UsuarioActual(
                                10L,
                                "SUPERVISOR",
                                4L
                        )
                );

        GestionarRelevoUseCase.Command preparado =
                service.prepararRegistro(command());

        assertEquals(9L, preparado.plazaId());
        assertEquals(99L, preparado.operadorId());
    }

    @Test
    void operadorNoPuedeActualizar() {
        RelevoAccesoService service =
                new RelevoAccesoService(
                        () -> new RelevoUsuarioActualPort.UsuarioActual(
                                55L,
                                "OPERADOR",
                                4L
                        )
                );

        assertThrows(
                ForbiddenException.class,
                service::exigirPuedeActualizar
        );
    }

    private GestionarRelevoUseCase.Command command() {
        return new GestionarRelevoUseCase.Command(
                9L,
                1L,
                99L,
                LocalDate.of(2026, 9, 23),
                LocalTime.of(8, 0),
                null,
                null,
                List.of(
                        new GestionarRelevoUseCase.ChecklistItem(
                                1L,
                                com.sigo.relevo.domain.EstadoRelevo.OPERATIVO,
                                null,
                                1
                        )
                ),
                List.of()
        );
    }
}
