package com.sigo.inventario.application.service;

import com.sigo.inventario.application.port.in.CerrarInventarioUseCase;
import com.sigo.inventario.application.port.in.InventarioConsultaUseCase;
import com.sigo.inventario.application.port.out.InventarioAuditoriaPort;
import com.sigo.inventario.application.port.out.InventarioCierrePort;
import com.sigo.inventario.application.port.out.InventarioConsultaPort;
import com.sigo.inventario.domain.InventarioEstado;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CerrarInventarioServiceTest {

    @Test
    void finalizaCuandoTodosLosProductosFueronContados() {
        StubCierrePort cierrePort = new StubCierrePort();

        CerrarInventarioService service =
                new CerrarInventarioService(
                        new StubConsultaPort(),
                        cierrePort,
                        new StubConsultaUseCase(),
                        new InventarioAuditoriaService(
                                new StubAuditoriaPort()
                        )
                );

        var detalle = service.finalizar(
                new CerrarInventarioUseCase.Usuario(
                        10L,
                        4L,
                        "CONTROLADOR"
                ),
                1L
        );

        assertEquals(1L, cierrePort.finalizadoId);
        assertEquals(InventarioEstado.FINALIZADO, detalle.estado());
    }

    @Test
    void noFinalizaSiFaltaUnProducto() {
        CerrarInventarioService service =
                new CerrarInventarioService(
                        new StubConsultaPort(true),
                        new StubCierrePort(),
                        new StubConsultaUseCase(),
                        new InventarioAuditoriaService(
                                new StubAuditoriaPort()
                        )
                );

        assertThrows(
                ResponseStatusException.class,
                () -> service.finalizar(
                        new CerrarInventarioUseCase.Usuario(
                                10L,
                                4L,
                                "CONTROLADOR"
                        ),
                        1L
                )
        );
    }

    private static class StubCierrePort
            implements InventarioCierrePort {

        private Long finalizadoId;

        @Override
        public void finalizar(Long inventarioId) {
            finalizadoId = inventarioId;
        }

        @Override
        public void anular(
                Long inventarioId,
                Long usuarioId,
                String motivo
        ) {
        }
    }

    private static class StubAuditoriaPort
            implements InventarioAuditoriaPort {

        @Override
        public void registrar(
                Long usuarioId,
                String accion,
                String entidad,
                Long entidadId,
                Map<String, Object> detalle
        ) {
        }
    }

    private static class StubConsultaPort
            implements InventarioConsultaPort {

        private final boolean agregarFaltante;

        StubConsultaPort() {
            this(false);
        }

        StubConsultaPort(boolean agregarFaltante) {
            this.agregarFaltante = agregarFaltante;
        }

        @Override
        public Cabecera requireInventario(Long inventarioId) {
            return new Cabecera(
                    inventarioId,
                    4L,
                    "P4",
                    10L,
                    287,
                    "Controlador",
                    2L,
                    "CONTROLADOR",
                    OffsetDateTime.now(),
                    null,
                    InventarioEstado.EN_PROCESO,
                    null,
                    null
            );
        }

        @Override
        public List<InventarioConsultaUseCase.Producto> productosPermitidos(
                Long rolId,
                Long plazaId
        ) {
            if (agregarFaltante) {
                return List.of(
                        producto(100L),
                        producto(200L)
                );
            }

            return List.of(producto(100L));
        }

        @Override
        public List<InventarioConsultaUseCase.DetalleItem> detalleItems(
                Long inventarioId
        ) {
            return List.of(
                    new InventarioConsultaUseCase.DetalleItem(
                            100L,
                            "Conos",
                            "UNIDAD",
                            null,
                            null,
                            BigDecimal.ONE
                    )
            );
        }

        @Override
        public Optional<Long> rolActivoId(String codigo) {
            return Optional.of(2L);
        }

        @Override
        public InventarioConsultaUseCase.Pagina<InventarioConsultaUseCase.Resumen> historial(
                FiltroHistorial filtro
        ) {
            return new InventarioConsultaUseCase.Pagina<>(
                    List.of(),
                    0,
                    10,
                    0,
                    0
            );
        }

        private InventarioConsultaUseCase.Producto producto(Long id) {
            return new InventarioConsultaUseCase.Producto(
                    id,
                    "P" + id,
                    "Producto " + id,
                    null,
                    "UNIDAD",
                    null,
                    null,
                    null,
                    null
            );
        }
    }

    private static class StubConsultaUseCase
            implements InventarioConsultaUseCase {

        @Override
        public List<Producto> productosPermitidos(
                Usuario usuario,
                Long inventarioId
        ) {
            return List.of();
        }

        @Override
        public Detalle detalle(
                Usuario usuario,
                Long inventarioId
        ) {
            return new Detalle(
                    inventarioId,
                    4L,
                    "P4",
                    10L,
                    287,
                    "Controlador",
                    "CONTROLADOR",
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    InventarioEstado.FINALIZADO,
                    null,
                    null,
                    List.of()
            );
        }

        @Override
        public Pagina<Resumen> historial(
                Usuario usuario,
                Long plazaId,
                Long responsableId,
                String rol,
                InventarioEstado estado,
                LocalDate desde,
                LocalDate hasta,
                int page,
                int size
        ) {
            return new Pagina<>(
                    List.of(),
                    page,
                    size,
                    0,
                    0
            );
        }
    }
}
