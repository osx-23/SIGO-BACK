package com.sigo.inventario.infrastructure.persistence.adapter;

import com.sigo.inventario.application.port.in.CerrarInventarioUseCase;
import com.sigo.inventario.application.port.in.GuardarConteoInventarioUseCase;
import com.sigo.inventario.application.port.in.IniciarInventarioUseCase;
import com.sigo.inventario.application.port.in.InventarioConsultaUseCase;
import com.sigo.inventario.application.port.out.InventarioAuditoriaPort;
import com.sigo.inventario.application.service.CerrarInventarioService;
import com.sigo.inventario.application.service.GuardarConteoInventarioService;
import com.sigo.inventario.application.service.IniciarInventarioService;
import com.sigo.inventario.application.service.InventarioConsultaService;
import com.sigo.inventario.domain.InventarioEstado;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioProducto;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioProductoPlaza;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioProductoRol;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioRol;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioConteoDetalleRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioConteoRepository;
import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Puesto;
import com.sigo.personal.infrastructure.persistence.entity.RolSistema;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@Import({
        IniciarInventarioJpaAdapter.class,
        InventarioConsultaJpaAdapter.class,
        InventarioDetalleGestionJpaAdapter.class,
        InventarioProductoVisibilidadJpaAdapter.class,
        InventarioCierreJpaAdapter.class
})
@Testcontainers(disabledWithoutDocker = true)
class InventarioFlujoJpaIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("sigo_inventario_test")
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
    private IniciarInventarioJpaAdapter iniciarAdapter;

    @Autowired
    private InventarioConsultaJpaAdapter consultaAdapter;

    @Autowired
    private InventarioDetalleGestionJpaAdapter detalleAdapter;

    @Autowired
    private InventarioProductoVisibilidadJpaAdapter visibilidadAdapter;

    @Autowired
    private InventarioCierreJpaAdapter cierreAdapter;

    @Autowired
    private InventarioConteoRepository conteoRepository;

    @Autowired
    private InventarioConteoDetalleRepository detalleRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Plaza plaza;
    private Trabajador responsable;
    private InventarioRol rol;
    private InventarioProducto producto1;
    private InventarioProducto producto2;

    private IniciarInventarioService iniciarService;
    private GuardarConteoInventarioService guardarService;
    private CerrarInventarioService cerrarService;
    private InventarioConsultaService consultaService;

    @BeforeEach
    void prepararDatos() {
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

        responsable = new Trabajador();
        responsable.setCodigo(9301);
        responsable.setNombreCompleto("Agente Inventario");
        responsable.setPuesto(puesto);
        responsable.setPlaza(plaza);
        responsable.setRolSistema(RolSistema.OPERADOR);
        responsable.setRequiereCambioPassword(false);
        responsable.setActivo(true);
        responsable = entityManager.persistAndFlush(
                responsable
        );

        rol = entityManager.persistAndFlush(
                new InventarioRol(
                        null,
                        "AGENTE",
                        "Agente",
                        true
                )
        );

        producto1 = producto(
                "CONO",
                "Cono de seguridad",
                "UND"
        );

        producto2 = producto(
                "PILA",
                "Pila",
                "UND"
        );

        autorizar(producto1);
        autorizar(producto2);

        consultaService =
                new InventarioConsultaService(
                        consultaAdapter
                );

        InventarioAuditoriaPort auditoria =
                new AuditoriaNoOp();

        iniciarService =
                new IniciarInventarioService(
                        iniciarAdapter,
                        auditoria
                );

        guardarService =
                new GuardarConteoInventarioService(
                        consultaAdapter,
                        detalleAdapter,
                        visibilidadAdapter,
                        consultaService,
                        auditoria
                );

        cerrarService =
                new CerrarInventarioService(
                        consultaAdapter,
                        cierreAdapter,
                        consultaService,
                        auditoria
                );
    }

    @Test
    void flujoCompletoIniciarContarFinalizarYConsultar() {
        var usuarioInicio =
                new IniciarInventarioUseCase.Usuario(
                        responsable.getId(),
                        plaza.getId(),
                        rol.getId(),
                        rol.getCodigo()
                );

        var iniciado = iniciarService.iniciar(
                usuarioInicio
        );

        assertNotNull(iniciado.id());
        assertEquals(
                InventarioEstado.EN_PROCESO,
                iniciado.estado()
        );

        var guardado = guardarService.guardar(
                usuarioConteo(),
                iniciado.id(),
                List.of(
                        new GuardarConteoInventarioUseCase.Item(
                                producto1.getId(),
                                new BigDecimal("12")
                        ),
                        new GuardarConteoInventarioUseCase.Item(
                                producto2.getId(),
                                new BigDecimal("4.50")
                        )
                )
        );

        assertEquals(2, guardado.productos().size());
        assertEquals(
                2,
                detalleRepository.countByInventarioId(
                        iniciado.id()
                )
        );

        var finalizado = cerrarService.finalizar(
                usuarioCierre(),
                iniciado.id()
        );

        assertEquals(
                InventarioEstado.FINALIZADO,
                finalizado.estado()
        );
        assertNotNull(finalizado.fechaFinalizacion());

        var consultado = consultaService.detalle(
                usuarioConsulta(),
                iniciado.id()
        );

        assertEquals(
                InventarioEstado.FINALIZADO,
                consultado.estado()
        );
        assertEquals(2, consultado.productos().size());

        var historial = consultaService.historial(
                usuarioConsulta(),
                plaza.getId(),
                responsable.getId(),
                rol.getCodigo(),
                InventarioEstado.FINALIZADO,
                null,
                null,
                0,
                20
        );

        assertEquals(1, historial.totalElementos());
        assertEquals(
                iniciado.id(),
                historial.contenido().get(0).id()
        );
    }

    @Test
    void noPermiteSegundoInventarioEnProcesoDelMismoResponsable() {
        var usuario =
                new IniciarInventarioUseCase.Usuario(
                        responsable.getId(),
                        plaza.getId(),
                        rol.getId(),
                        rol.getCodigo()
                );

        iniciarService.iniciar(usuario);

        assertThrows(
                ConflictException.class,
                () -> iniciarService.iniciar(usuario)
        );

        assertEquals(
                1,
                conteoRepository.count()
        );
    }

    @Test
    void noFinalizaHastaContarTodosLosProductosPermitidos() {
        var iniciado = iniciarService.iniciar(
                new IniciarInventarioUseCase.Usuario(
                        responsable.getId(),
                        plaza.getId(),
                        rol.getId(),
                        rol.getCodigo()
                )
        );

        guardarService.guardar(
                usuarioConteo(),
                iniciado.id(),
                List.of(
                        new GuardarConteoInventarioUseCase.Item(
                                producto1.getId(),
                                BigDecimal.TEN
                        )
                )
        );

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> cerrarService.finalizar(
                        usuarioCierre(),
                        iniciado.id()
                )
        );

        assertTrue(
                error.getMessage().contains(
                        "Faltan productos por contar"
                )
        );

        assertEquals(
                InventarioEstado.EN_PROCESO,
                consultaService.detalle(
                        usuarioConsulta(),
                        iniciado.id()
                ).estado()
        );
    }

    @Test
    void rechazaProductoDuplicadoSinPersistirDetalleParcial() {
        var iniciado = iniciarService.iniciar(
                new IniciarInventarioUseCase.Usuario(
                        responsable.getId(),
                        plaza.getId(),
                        rol.getId(),
                        rol.getCodigo()
                )
        );

        assertThrows(
                BusinessException.class,
                () -> guardarService.guardar(
                        usuarioConteo(),
                        iniciado.id(),
                        List.of(
                                new GuardarConteoInventarioUseCase.Item(
                                        producto1.getId(),
                                        BigDecimal.ONE
                                ),
                                new GuardarConteoInventarioUseCase.Item(
                                        producto1.getId(),
                                        BigDecimal.TEN
                                )
                        )
                )
        );

        assertEquals(
                0,
                detalleRepository.countByInventarioId(
                        iniciado.id()
                )
        );
    }

    @Test
    void permiteActualizarConteoSinDuplicarProducto() {
        var iniciado = iniciarService.iniciar(
                new IniciarInventarioUseCase.Usuario(
                        responsable.getId(),
                        plaza.getId(),
                        rol.getId(),
                        rol.getCodigo()
                )
        );

        guardarService.guardar(
                usuarioConteo(),
                iniciado.id(),
                List.of(
                        new GuardarConteoInventarioUseCase.Item(
                                producto1.getId(),
                                new BigDecimal("3")
                        )
                )
        );

        guardarService.guardar(
                usuarioConteo(),
                iniciado.id(),
                List.of(
                        new GuardarConteoInventarioUseCase.Item(
                                producto1.getId(),
                                new BigDecimal("8")
                        )
                )
        );

        assertEquals(
                1,
                detalleRepository.countByInventarioId(
                        iniciado.id()
                )
        );

        var detalle = consultaService.detalle(
                usuarioConsulta(),
                iniciado.id()
        );

        assertEquals(
                new BigDecimal("8.00"),
                detalle.productos()
                        .stream()
                        .filter(item ->
                                item.productoId()
                                        .equals(producto1.getId())
                        )
                        .findFirst()
                        .orElseThrow()
                        .cantidad()
        );
    }

    @Test
    void anularInventarioGuardaEstadoYMotivo() {
        var iniciado = iniciarService.iniciar(
                new IniciarInventarioUseCase.Usuario(
                        responsable.getId(),
                        plaza.getId(),
                        rol.getId(),
                        rol.getCodigo()
                )
        );

        var anulado = cerrarService.anular(
                usuarioCierre(),
                iniciado.id(),
                "  Conteo iniciado por error  "
        );

        assertEquals(
                InventarioEstado.ANULADO,
                anulado.estado()
        );
        assertEquals(
                "Conteo iniciado por error",
                anulado.motivoAnulacion()
        );
    }

    private InventarioProducto producto(
            String codigo,
            String nombre,
            String unidad
    ) {
        InventarioProducto producto =
                new InventarioProducto();

        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setUnidadMedida(unidad);
        producto.setActivo(true);

        return entityManager.persistAndFlush(
                producto
        );
    }

    private void autorizar(
            InventarioProducto producto
    ) {
        entityManager.persistAndFlush(
                new InventarioProductoPlaza(
                        producto,
                        plaza,
                        0
                )
        );

        entityManager.persistAndFlush(
                new InventarioProductoRol(
                        producto,
                        rol
                )
        );
    }

    private GuardarConteoInventarioUseCase.Usuario usuarioConteo() {
        return new GuardarConteoInventarioUseCase.Usuario(
                responsable.getId(),
                plaza.getId(),
                rol.getId(),
                rol.getCodigo()
        );
    }

    private CerrarInventarioUseCase.Usuario usuarioCierre() {
        return new CerrarInventarioUseCase.Usuario(
                responsable.getId(),
                plaza.getId(),
                rol.getCodigo()
        );
    }

    private InventarioConsultaUseCase.Usuario usuarioConsulta() {
        return new InventarioConsultaUseCase.Usuario(
                responsable.getId(),
                plaza.getId(),
                rol.getCodigo()
        );
    }

    private static class AuditoriaNoOp
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
