package com.sigo.asistencia.application.service;

import com.sigo.asistencia.application.port.in.ConsultarAsistenciasUseCase;
import com.sigo.asistencia.application.port.in.GestionarAsistenciaUseCase;
import com.sigo.asistencia.application.port.out.AsistenciaGestionPort;
import com.sigo.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GestionarAsistenciaServiceTest {

    @Test
    void registraCuandoAusenciasCoincidenConLaDiferencia() {
        StubGestionPort port = new StubGestionPort();
        GestionarAsistenciaService service =
                new GestionarAsistenciaService(
                        port,
                        new StubConsultaUseCase()
                );

        var resultado = service.registrar(
                command(
                        7,
                        6,
                        List.of(
                                new GestionarAsistenciaUseCase.AusenciaCommand(
                                        99L,
                                        1L,
                                        "Falta"
                                )
                        )
                )
        );

        assertEquals(10L, port.registradoId);
        assertEquals(10L, resultado.id());
    }

    @Test
    void rechazaCantidadIncorrectaDeAusencias() {
        GestionarAsistenciaService service =
                new GestionarAsistenciaService(
                        new StubGestionPort(),
                        new StubConsultaUseCase()
                );

        assertThrows(
                BusinessException.class,
                () -> service.registrar(
                        command(
                                7,
                                5,
                                List.of(
                                        new GestionarAsistenciaUseCase.AusenciaCommand(
                                                99L,
                                                1L,
                                                null
                                        )
                                )
                        )
                )
        );
    }

    @Test
    void rechazaTrabajadorAusenteDuplicado() {
        GestionarAsistenciaService service =
                new GestionarAsistenciaService(
                        new StubGestionPort(),
                        new StubConsultaUseCase()
                );

        assertThrows(
                BusinessException.class,
                () -> service.registrar(
                        command(
                                7,
                                5,
                                List.of(
                                        new GestionarAsistenciaUseCase.AusenciaCommand(
                                                99L,
                                                1L,
                                                null
                                        ),
                                        new GestionarAsistenciaUseCase.AusenciaCommand(
                                                99L,
                                                2L,
                                                null
                                        )
                                )
                        )
                )
        );
    }

    private GestionarAsistenciaUseCase.Command command(
            int programados,
            int presentes,
            List<GestionarAsistenciaUseCase.AusenciaCommand> ausencias
    ) {
        return new GestionarAsistenciaUseCase.Command(
                4L,
                1L,
                20L,
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

    private static class StubGestionPort
            implements AsistenciaGestionPort {

        private Long registradoId;

        @Override
        public Long registrar(
                GestionarAsistenciaUseCase.Command command
        ) {
            registradoId = 10L;
            return registradoId;
        }

        @Override
        public Long actualizar(
                Long id,
                GestionarAsistenciaUseCase.Command command
        ) {
            return id;
        }
    }

    private static class StubConsultaUseCase
            implements ConsultarAsistenciasUseCase {

        @Override
        public Asistencia obtenerPorId(Long id) {
            return new Asistencia(
                    id,
                    4L,
                    "P4",
                    1L,
                    "A",
                    20L,
                    "Controlador",
                    LocalDate.of(2026, 9, 23),
                    7,
                    6,
                    1,
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
