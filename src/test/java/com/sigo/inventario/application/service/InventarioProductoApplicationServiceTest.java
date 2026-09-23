package com.sigo.inventario.application.service;

import com.sigo.inventario.application.port.in.InventarioProductoUseCase;
import com.sigo.inventario.application.port.out.InventarioAuditoriaPort;
import com.sigo.inventario.application.port.out.InventarioProductoGestionPort;
import org.junit.jupiter.api.Test;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ForbiddenException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventarioProductoApplicationServiceTest {

    @Test
    void normalizaProductoAntesDeCrear() {
        StubPort port = new StubPort();
        InventarioProductoApplicationService service =
                new InventarioProductoApplicationService(
                        port,
                        new StubAuditoriaPort()
                );

        var producto = service.crear(
                new InventarioProductoUseCase.Usuario(
                        10L,
                        4L,
                        "SUPERVISOR"
                ),
                new InventarioProductoUseCase.Command(
                        " cono-01 ",
                        " Cono grande ",
                        " prueba ",
                        1L,
                        2L,
                        " unidad ",
                        null,
                        Set.of(" controlador "),
                        List.of(
                                new InventarioProductoUseCase.PlazaConfig(
                                        4L,
                                        3
                                )
                        )
                )
        );

        assertEquals("CONO-01", port.command.codigo());
        assertEquals("Cono grande", port.command.nombre());
        assertEquals(Boolean.TRUE, port.command.activo());
        assertEquals(Set.of("CONTROLADOR"), port.command.roles());
        assertEquals(50L, producto.id());
    }

    @Test
    void controladorNoPuedeAsignarRolSupervisor() {
        InventarioProductoApplicationService service =
                new InventarioProductoApplicationService(
                        new StubPort(),
                        new StubAuditoriaPort()
                );

        assertThrows(
                ForbiddenException.class,
                () -> service.crear(
                        new InventarioProductoUseCase.Usuario(
                                10L,
                                4L,
                                "CONTROLADOR"
                        ),
                        new InventarioProductoUseCase.Command(
                                "C1",
                                "Cono",
                                null,
                                1L,
                                2L,
                                "UNIDAD",
                                true,
                                Set.of("SUPERVISOR"),
                                List.of(
                                        new InventarioProductoUseCase.PlazaConfig(
                                                4L,
                                                0
                                        )
                                )
                        )
                )
        );
    }

    @Test
    void rechazaPlazaDuplicada() {
        InventarioProductoApplicationService service =
                new InventarioProductoApplicationService(
                        new StubPort(),
                        new StubAuditoriaPort()
                );

        assertThrows(
                BusinessException.class,
                () -> service.crear(
                        new InventarioProductoUseCase.Usuario(
                                10L,
                                null,
                                "SUPERVISOR"
                        ),
                        new InventarioProductoUseCase.Command(
                                "C1",
                                "Cono",
                                null,
                                1L,
                                2L,
                                "UNIDAD",
                                true,
                                Set.of("OPERADOR"),
                                List.of(
                                        new InventarioProductoUseCase.PlazaConfig(
                                                4L,
                                                0
                                        ),
                                        new InventarioProductoUseCase.PlazaConfig(
                                                4L,
                                                1
                                        )
                                )
                        )
                )
        );
    }

    private static class StubPort
            implements InventarioProductoGestionPort {

        private InventarioProductoUseCase.Command command;

        @Override
        public List<InventarioProductoUseCase.Producto> listar(
                Long plazaId
        ) {
            return List.of();
        }

        @Override
        public InventarioProductoUseCase.Producto crear(
                InventarioProductoUseCase.Usuario usuario,
                InventarioProductoUseCase.Command command
        ) {
            this.command = command;
            return producto(command);
        }

        @Override
        public InventarioProductoUseCase.Producto actualizar(
                InventarioProductoUseCase.Usuario usuario,
                Long productoId,
                InventarioProductoUseCase.Command command
        ) {
            this.command = command;
            return producto(command);
        }

        @Override
        public InventarioProductoUseCase.Producto cambiarEstado(
                InventarioProductoUseCase.Usuario usuario,
                Long productoId,
                boolean activo
        ) {
            return new InventarioProductoUseCase.Producto(
                    productoId,
                    "C1",
                    "Cono",
                    null,
                    1L,
                    "Seguridad",
                    2L,
                    "Plaza",
                    "UNIDAD",
                    activo,
                    Set.of("OPERADOR"),
                    List.of()
            );
        }

        private InventarioProductoUseCase.Producto producto(
                InventarioProductoUseCase.Command command
        ) {
            return new InventarioProductoUseCase.Producto(
                    50L,
                    command.codigo(),
                    command.nombre(),
                    command.descripcion(),
                    command.categoriaId(),
                    "Seguridad",
                    command.ambitoId(),
                    "Plaza",
                    command.unidadMedida(),
                    command.activo(),
                    command.roles(),
                    List.of()
            );
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
}
