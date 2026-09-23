package com.sigo.inventario.infrastructure.persistence.repository;

import com.sigo.inventario.application.port.in.InventarioStockUseCase;
import com.sigo.inventario.infrastructure.persistence.entity.EstadoInventario;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioConteo;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioConteoDetalle;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioProducto;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioProductoPlaza;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioRol;
import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Puesto;
import com.sigo.personal.infrastructure.persistence.entity.RolSistema;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.TestConfiguration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@Import({
        InventarioStockRepository.class,
        InventarioStockRepositoryIT.JdbcConfig.class
})
@Testcontainers(disabledWithoutDocker = true)
class InventarioStockRepositoryIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("sigo_stock_test")
                    .withUsername("sigo")
                    .withPassword("sigo");

    @DynamicPropertySource
    static void datasource(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );
        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );
        registry.add(
                "spring.datasource.driver-class-name",
                POSTGRES::getDriverClassName
        );
    }

    @Autowired
    private InventarioStockRepository repository;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private TestEntityManager entityManager;

    private Plaza plaza;
    private InventarioProducto producto;

    @BeforeEach
    void prepararDatosYVista() {
        plaza = entityManager.persistAndFlush(
                new Plaza(
                        null,
                        "P4",
                        "Plaza 4",
                        true
                )
        );

        Puesto puesto = entityManager.persistAndFlush(
                new Puesto(
                        null,
                        "Agente de Recaudación"
                )
        );

        Trabajador trabajador = new Trabajador();
        trabajador.setCodigo(9401);
        trabajador.setNombreCompleto("Agente Stock");
        trabajador.setPuesto(puesto);
        trabajador.setPlaza(plaza);
        trabajador.setRolSistema(RolSistema.OPERADOR);
        trabajador.setRequiereCambioPassword(false);
        trabajador.setActivo(true);
        trabajador = entityManager.persistAndFlush(
                trabajador
        );

        InventarioRol rol = entityManager.persistAndFlush(
                new InventarioRol(
                        null,
                        "AGENTE",
                        "Agente",
                        true
                )
        );

        producto = new InventarioProducto();
        producto.setCodigo("CONO-STOCK");
        producto.setNombre("Cono Stock");
        producto.setUnidadMedida("UND");
        producto.setActivo(true);
        producto = entityManager.persistAndFlush(
                producto
        );

        entityManager.persistAndFlush(
                new InventarioProductoPlaza(
                        producto,
                        plaza,
                        10
                )
        );

        InventarioConteo inventario =
                new InventarioConteo();
        inventario.setPlaza(plaza);
        inventario.setResponsable(trabajador);
        inventario.setRol(rol);
        inventario.setEstado(EstadoInventario.FINALIZADO);
        inventario.setFechaFinalizacion(
                OffsetDateTime.now()
        );
        inventario = entityManager.persistAndFlush(
                inventario
        );

        InventarioConteoDetalle detalle =
                new InventarioConteoDetalle();
        detalle.setInventario(inventario);
        detalle.setProducto(producto);
        detalle.setCantidadEncontrada(
                new BigDecimal("7.00")
        );
        detalle.setNombreProductoSnapshot(
                producto.getNombre()
        );
        detalle.setUnidadSnapshot(
                producto.getUnidadMedida()
        );
        entityManager.persistAndFlush(detalle);

        jdbc.getJdbcTemplate().execute(
                """
                CREATE OR REPLACE VIEW v_inventario_stock_actual AS
                SELECT DISTINCT ON (i.plaza_id, d.producto_id)
                       i.plaza_id,
                       pl.codigo AS plaza,
                       d.producto_id,
                       d.nombre_producto_snapshot AS producto,
                       d.unidad_snapshot AS unidad_medida,
                       d.cantidad_encontrada AS cantidad_actual,
                       i.id AS inventario_id,
                       i.fecha_finalizacion AS actualizado_en
                FROM inventario_conteo i
                JOIN plazas pl ON pl.id = i.plaza_id
                JOIN inventario_conteo_detalle d
                  ON d.inventario_id = i.id
                WHERE i.estado = 'FINALIZADO'
                ORDER BY i.plaza_id,
                         d.producto_id,
                         i.fecha_finalizacion DESC NULLS LAST,
                         i.id DESC
                """
        );
    }

    @Test
    void consultaStockActualYDetectaBajoMinimo() {
        List<InventarioStockUseCase.Stock> stock =
                repository.buscar(
                        plaza.getId(),
                        null
                );

        assertEquals(1, stock.size());

        var item = stock.get(0);

        assertEquals(plaza.getId(), item.plazaId());
        assertEquals(producto.getId(), item.productoId());
        assertEquals(
                new BigDecimal("7.00"),
                item.cantidadActual()
        );
        assertEquals(10, item.stockMinimo());
        assertTrue(item.bajoMinimo());
        assertNotNull(item.inventarioId());
        assertNotNull(item.actualizadoEn());
    }

    @Test
    void filtroPorTextoNoDevuelveProductosQueNoCoinciden() {
        assertTrue(
                repository.buscar(
                        plaza.getId(),
                        "producto inexistente"
                ).isEmpty()
        );

        assertEquals(
                1,
                repository.buscar(
                        plaza.getId(),
                        "cono"
                ).size()
        );
    }

    @TestConfiguration
    static class JdbcConfig {

        @Bean
        NamedParameterJdbcTemplate namedParameterJdbcTemplate(
                DataSource dataSource
        ) {
            return new NamedParameterJdbcTemplate(
                    dataSource
            );
        }
    }
}
