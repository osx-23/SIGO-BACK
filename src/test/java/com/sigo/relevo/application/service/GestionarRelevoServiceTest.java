package com.sigo.relevo.application.service;

import com.sigo.relevo.application.port.in.ConsultarRelevosUseCase;
import com.sigo.relevo.application.port.in.GestionarRelevoUseCase;
import com.sigo.relevo.application.port.out.RelevoGestionPort;
import com.sigo.relevo.domain.EstadoRelevo;
import com.sigo.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GestionarRelevoServiceTest {

    @Test
    void registraChecklistCompletoYViaValida() {
        StubGestionPort port = new StubGestionPort();
        GestionarRelevoService service =
                new GestionarRelevoService(
                        port,
                        new StubConsulta()
                );

        var resultado = service.registrar(
                new GestionarRelevoUseCase.Command(
                        4L,
                        1L,
                        99L,
                        LocalDate.of(2026, 9, 23),
                        LocalTime.of(8, 30),
                        "  ok  ",
                        "  relevo  ",
                        List.of(
                                new GestionarRelevoUseCase.ChecklistItem(
                                        1L,
                                        EstadoRelevo.OPERATIVO,
                                        null,
                                        null
                                ),
                                new GestionarRelevoUseCase.ChecklistItem(
                                        2L,
                                        EstadoRelevo.OBSERVADO,
                                        " detalle ",
                                        4
                                )
                        ),
                        List.of(
                                new GestionarRelevoUseCase.ViaItem(
                                        10L,
                                        EstadoRelevo.OPERATIVO,
                                        null
                                )
                        )
                )
        );

        assertEquals(50L, resultado.id());
        assertEquals("ok", port.command.observaciones());
        assertEquals(
                "detalle",
                port.command.checklist().get(1).detalle()
        );
    }

    @Test
    void rechazaChecklistIncompleto() {
        GestionarRelevoService service =
                new GestionarRelevoService(
                        new StubGestionPort(),
                        new StubConsulta()
                );

        assertThrows(
                BusinessException.class,
                () -> service.registrar(
                        new GestionarRelevoUseCase.Command(
                                4L,
                                1L,
                                99L,
                                LocalDate.of(2026, 9, 23),
                                LocalTime.of(8, 30),
                                null,
                                null,
                                List.of(
                                        new GestionarRelevoUseCase.ChecklistItem(
                                                1L,
                                                EstadoRelevo.OPERATIVO,
                                                null,
                                                null
                                        )
                                ),
                                List.of()
                        )
                )
        );
    }

    @Test
    void observadoRequiereDetalle() {
        GestionarRelevoService service =
                new GestionarRelevoService(
                        new StubGestionPort(),
                        new StubConsulta()
                );

        assertThrows(
                BusinessException.class,
                () -> service.registrar(
                        new GestionarRelevoUseCase.Command(
                                4L,
                                1L,
                                99L,
                                LocalDate.of(2026, 9, 23),
                                LocalTime.of(8, 30),
                                null,
                                null,
                                List.of(
                                        new GestionarRelevoUseCase.ChecklistItem(
                                                1L,
                                                EstadoRelevo.OBSERVADO,
                                                null,
                                                null
                                        ),
                                        new GestionarRelevoUseCase.ChecklistItem(
                                                2L,
                                                EstadoRelevo.OPERATIVO,
                                                null,
                                                1
                                        )
                                ),
                                List.of()
                        )
                )
        );
    }

    private static class StubGestionPort
            implements RelevoGestionPort {

        private GestionarRelevoUseCase.Command command;

        @Override
        public List<ElementoConfig> elementosActivos() {
            return List.of(
                    new ElementoConfig(
                            1L,
                            "Baños",
                            false
                    ),
                    new ElementoConfig(
                            2L,
                            "Conos",
                            true
                    )
            );
        }

        @Override
        public ViaConfig requireVia(Long viaId) {
            return new ViaConfig(
                    viaId,
                    4L,
                    1,
                    true
            );
        }

        @Override
        public Long registrar(
                GestionarRelevoUseCase.Command command
        ) {
            this.command = command;
            return 50L;
        }

        @Override
        public Long actualizar(
                Long id,
                GestionarRelevoUseCase.Command command
        ) {
            this.command = command;
            return id;
        }
    }

    private static class StubConsulta
            implements ConsultarRelevosUseCase {

        @Override
        public List<Elemento> listarElementos() {
            return List.of();
        }

        @Override
        public Relevo obtener(Long id) {
            return new Relevo(
                    id,
                    4L,
                    "P4",
                    "Plaza 4",
                    1L,
                    "A",
                    "Turno A",
                    99L,
                    100,
                    "Operador",
                    LocalDate.of(2026, 9, 23),
                    LocalTime.of(8, 30),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    List.of()
            );
        }

        @Override
        public List<Relevo> listar(
                LocalDate inicio,
                LocalDate fin
        ) {
            return List.of();
        }
    }
}
