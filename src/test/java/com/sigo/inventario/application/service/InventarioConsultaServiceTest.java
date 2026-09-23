package com.sigo.inventario.application.service;

import com.sigo.inventario.application.port.in.InventarioConsultaUseCase;
import com.sigo.inventario.application.port.out.InventarioConsultaPort;
import com.sigo.inventario.domain.InventarioEstado;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventarioConsultaServiceTest {

    @Test
    void propietarioPuedeConsultarProductosPermitidos() {
        InventarioConsultaService service =
                new InventarioConsultaService(
                        new StubConsultaPort()
                );

        var productos = service.productosPermitidos(
                new InventarioConsultaUseCase.Usuario(
                        10L,
                        4L,
                        "CONTROLADOR"
                ),
                1L
        );

        assertEquals(1, productos.size());
        assertEquals("CONOS", productos.get(0).codigo());
    }

    @Test
    void otroTrabajadorNoPuedeConsultarProductosDeInventarioAjeno() {
        InventarioConsultaService service =
                new InventarioConsultaService(
                        new StubConsultaPort()
                );

        assertThrows(
                ResponseStatusException.class,
                () -> service.productosPermitidos(
                        new InventarioConsultaUseCase.Usuario(
                                99L,
                                4L,
                                "CONTROLADOR"
                        ),
                        1L
                )
        );
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
                    "Controlador Test",
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
            return List.of(
                    new InventarioConsultaUseCase.Producto(
                            1L,
                            "CONOS",
                            "Conos",
                            null,
                            "UNIDAD",
                            null,
                            null,
                            null,
                            null
                    )
            );
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
                    filtro.pagina(),
                    filtro.tamanio(),
                    0,
                    0
            );
        }
    }
}
