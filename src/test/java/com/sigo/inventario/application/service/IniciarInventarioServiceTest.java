package com.sigo.inventario.application.service;

import com.sigo.inventario.application.port.in.IniciarInventarioUseCase;
import com.sigo.inventario.application.port.out.IniciarInventarioPort;
import com.sigo.inventario.application.port.out.InventarioAuditoriaPort;
import com.sigo.inventario.domain.InventarioEstado;
import org.junit.jupiter.api.Test;
import com.sigo.shared.exception.ConflictException;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IniciarInventarioServiceTest {

    @Test
    void iniciaInventarioCuandoNoExisteUnoAbierto() {
        StubIniciarPort iniciarPort = new StubIniciarPort();
        StubAuditoriaPort auditoriaPort = new StubAuditoriaPort();

        IniciarInventarioService service =
                new IniciarInventarioService(
                        iniciarPort,
                        new InventarioAuditoriaService(
                                auditoriaPort
                        )
                );

        var resumen = service.iniciar(
                new IniciarInventarioUseCase.Usuario(
                        10L,
                        4L,
                        2L,
                        "CONTROLADOR"
                )
        );

        assertEquals(100L, resumen.id());
        assertEquals(100L, auditoriaPort.entidadId);
        assertEquals("INVENTARIO_INICIADO", auditoriaPort.accion);
    }

    @Test
    void rechazaSegundoInventarioEnProceso() {
        StubIniciarPort iniciarPort = new StubIniciarPort();
        iniciarPort.abierto = 88L;

        IniciarInventarioService service =
                new IniciarInventarioService(
                        iniciarPort,
                        new InventarioAuditoriaService(
                                new StubAuditoriaPort()
                        )
                );

        assertThrows(
                ConflictException.class,
                () -> service.iniciar(
                        new IniciarInventarioUseCase.Usuario(
                                10L,
                                4L,
                                2L,
                                "CONTROLADOR"
                        )
                )
        );
    }

    private static class StubIniciarPort
            implements IniciarInventarioPort {

        private Long abierto;

        @Override
        public Optional<Long> inventarioEnProcesoId(
                Long responsableId
        ) {
            return Optional.ofNullable(abierto);
        }

        @Override
        public IniciarInventarioUseCase.Resumen crear(
                IniciarInventarioUseCase.Usuario usuario
        ) {
            return new IniciarInventarioUseCase.Resumen(
                    100L,
                    usuario.plazaId(),
                    "P4",
                    usuario.trabajadorId(),
                    287,
                    "Controlador Test",
                    usuario.rolCodigo(),
                    OffsetDateTime.now(),
                    null,
                    InventarioEstado.EN_PROCESO,
                    0
            );
        }
    }

    private static class StubAuditoriaPort
            implements InventarioAuditoriaPort {

        private String accion;
        private Long entidadId;

        @Override
        public void registrar(
                Long usuarioId,
                String accion,
                String entidad,
                Long entidadId,
                Map<String, Object> detalle
        ) {
            this.accion = accion;
            this.entidadId = entidadId;
        }
    }
}
