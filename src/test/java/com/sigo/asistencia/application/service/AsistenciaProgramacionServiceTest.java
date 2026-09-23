package com.sigo.asistencia.application.service;

import com.sigo.asistencia.application.port.out.AsistenciaProgramacionCatalogoPort;
import com.sigo.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AsistenciaProgramacionServiceTest {

    @Test
    void devuelveProgramadosDeP4TurnoA() {
        AsistenciaProgramacionService service =
                service("P4", "A");

        assertEquals(
                7,
                service.obtenerProgramados(1L, 1L)
        );
    }

    @Test
    void devuelveProgramadosDeP6TurnoB() {
        AsistenciaProgramacionService service =
                service("p6", " b ");

        assertEquals(
                12,
                service.obtenerProgramados(1L, 1L)
        );
    }

    @Test
    void rechazaPlazaSinConfiguracion() {
        AsistenciaProgramacionService service =
                service("PX", "A");

        assertThrows(
                BusinessException.class,
                () -> service.obtenerProgramados(1L, 1L)
        );
    }

    private AsistenciaProgramacionService service(
            String plaza,
            String turno
    ) {
        AsistenciaProgramacionCatalogoPort port =
                new AsistenciaProgramacionCatalogoPort() {
                    @Override
                    public String requirePlazaCodigo(Long plazaId) {
                        return plaza;
                    }

                    @Override
                    public String requireTurnoCodigo(Long turnoId) {
                        return turno;
                    }
                };

        return new AsistenciaProgramacionService(port);
    }
}
