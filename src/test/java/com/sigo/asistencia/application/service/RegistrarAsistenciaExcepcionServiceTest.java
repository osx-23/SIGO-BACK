package com.sigo.asistencia.application.service;

import com.sigo.asistencia.application.port.in.ConsultarAsistenciasUseCase;
import com.sigo.asistencia.application.port.in.GestionarAsistenciaUseCase;
import com.sigo.asistencia.application.port.out.AsistenciaExcepcionPort;
import com.sigo.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistrarAsistenciaExcepcionServiceTest {

    @Test
    void registraUsandoPuertoDeExcepcion() {
        StubPort port = new StubPort();
        RegistrarAsistenciaExcepcionService service =
                new RegistrarAsistenciaExcepcionService(
                        port,
                        new StubConsulta()
                );

        var resultado = service.registrar(
                comando(
                        7,
                        7,
                        List.of()
                )
        );

        assertEquals(77L, port.id);
        assertEquals(77L, resultado.id());
    }

    @Test
    void rechazaAusenciasDuplicadas() {
        RegistrarAsistenciaExcepcionService service =
                new RegistrarAsistenciaExcepcionService(
                        new StubPort(),
                        new StubConsulta()
                );

        assertThrows(
                BusinessException.class,
                () -> service.registrar(
                        comando(
                                7,
                                5,
                                List.of(
                                        new GestionarAsistenciaUseCase.AusenciaCommand(
                                                9L,
                                                1L,
                                                null
                                        ),
                                        new GestionarAsistenciaUseCase.AusenciaCommand(
                                                9L,
                                                2L,
                                                null
                                        )
                                )
                        )
                )
        );
    }

    private GestionarAsistenciaUseCase.Command comando(
            int programados,
            int presentes,
            List<GestionarAsistenciaUseCase.AusenciaCommand> ausencias
    ) {
        return new GestionarAsistenciaUseCase.Command(
                4L,
                1L,
                500L,
                LocalDate.of(2026, 9, 23),
                programados,
                presentes,
                0,
                null,
                null,
                ausencias,
                List.of()
        );
    }

    private static class StubPort
            implements AsistenciaExcepcionPort {

        private Long id;

        @Override
        public Long registrar(
                GestionarAsistenciaUseCase.Command command
        ) {
            id = 77L;
            return id;
        }
    }

    private static class StubConsulta
            implements ConsultarAsistenciasUseCase {

        @Override
        public Asistencia obtenerPorId(Long id) {
            return new Asistencia(
                    id,
                    4L,
                    "P4",
                    1L,
                    "A",
                    500L,
                    "Controlador Externo",
                    LocalDate.of(2026, 9, 23),
                    7,
                    7,
                    0,
                    0,
                    null,
                    null,
                    null,
                    List.of(),
                    List.of()
            );
        }

        @Override
        public List<Asistencia> listar(
                LocalDate inicio,
                LocalDate fin,
                Long plazaId
        ) {
            return List.of();
        }
    }
}
