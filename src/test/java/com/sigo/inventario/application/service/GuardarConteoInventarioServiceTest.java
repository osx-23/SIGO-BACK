package com.sigo.inventario.application.service;

import com.sigo.inventario.application.port.in.GuardarConteoInventarioUseCase;
import com.sigo.inventario.application.port.in.InventarioConsultaUseCase;
import com.sigo.inventario.application.port.out.InventarioAuditoriaPort;
import com.sigo.inventario.application.port.out.InventarioConsultaPort;
import com.sigo.inventario.application.port.out.InventarioDetalleGestionPort;
import com.sigo.inventario.application.port.out.InventarioProductoVisibilidadPort;
import com.sigo.inventario.domain.InventarioEstado;
import org.junit.jupiter.api.Test;
import com.sigo.shared.exception.BusinessException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuardarConteoInventarioServiceTest {

    @Test
    void guardaProductosValidosYAudita() {
        StubDetallePort detallePort = new StubDetallePort();
        StubAuditoriaPort auditoriaPort = new StubAuditoriaPort();

        GuardarConteoInventarioService service =
                new GuardarConteoInventarioService(
                        new StubConsultaPort(),
                        detallePort,
                        (productoId, rolId, plazaId) -> true,
                        new StubConsultaUseCase(),
                        auditoriaPort
                );

        var resultado = service.guardar(
                new GuardarConteoInventarioUseCase.Usuario(
                        10L,
                        4L,
                        2L,
                        "CONTROLADOR"
                ),
                1L,
                List.of(
                        new GuardarConteoInventarioUseCase.Item(
                                100L,
                                new BigDecimal("5")
                        )
                )
        );

        assertEquals(1, detallePort.productos.size());
        assertEquals(1L, resultado.id());
        assertEquals("CONTEO_GUARDADO", auditoriaPort.accion);
    }

    @Test
    void rechazaProductoDuplicado() {
        GuardarConteoInventarioService service =
                new GuardarConteoInventarioService(
                        new StubConsultaPort(),
                        new StubDetallePort(),
                        (productoId, rolId, plazaId) -> true,
                        new StubConsultaUseCase(),
                        new StubAuditoriaPort()
                );

        assertThrows(
                BusinessException.class,
                () -> service.guardar(
                        new GuardarConteoInventarioUseCase.Usuario(
                                10L,
                                4L,
                                2L,
                                "CONTROLADOR"
                        ),
                        1L,
                        List.of(
                                new GuardarConteoInventarioUseCase.Item(
                                        100L,
                                        BigDecimal.ONE
                                ),
                                new GuardarConteoInventarioUseCase.Item(
                                        100L,
                                        BigDecimal.TEN
                                )
                        )
                )
        );
    }

    private static class StubDetallePort
            implements InventarioDetalleGestionPort {

        private List<GuardarConteoInventarioUseCase.Item> productos;

        @Override
        public void guardar(
                Long inventarioId,
                List<GuardarConteoInventarioUseCase.Item> productos
        ) {
            this.productos = productos;
        }
    }

    private static class StubAuditoriaPort
            implements InventarioAuditoriaPort {

        private String accion;

        @Override
        public void registrar(
                Long usuarioId,
                String accion,
                String entidad,
                Long entidadId,
                Map<String, Object> detalle
        ) {
            this.accion = accion;
        }
    }

    private static class StubConsultaPort
            implements InventarioConsultaPort {

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
            return List.of();
        }

        @Override
        public List<InventarioConsultaUseCase.DetalleItem> detalleItems(
                Long inventarioId
        ) {
            return List.of();
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
                    null,
                    InventarioEstado.EN_PROCESO,
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
                java.time.LocalDate desde,
                java.time.LocalDate hasta,
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
