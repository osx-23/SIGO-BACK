package com.sigo.programacion.infrastructure.persistence.adapter;

import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Puesto;
import com.sigo.personal.infrastructure.persistence.entity.RolSistema;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.programacion.application.port.in.AsignarLiderUseCase;
import com.sigo.programacion.application.port.in.AsignarSecuenciaUseCase;
import com.sigo.programacion.application.port.in.DistribucionUseCase;
import com.sigo.programacion.application.port.in.GuardarOrdenSecuenciaUseCase;
import com.sigo.programacion.application.port.in.GuardarTurnosUseCase;
import com.sigo.programacion.application.port.in.UbicacionUseCase;
import com.sigo.programacion.application.port.out.ProgramacionAccessPort;
import com.sigo.programacion.application.service.DistribucionService;
import com.sigo.programacion.application.service.GuardarOrdenSecuenciaService;
import com.sigo.programacion.application.service.LiderService;
import com.sigo.programacion.application.service.MiHorarioService;
import com.sigo.programacion.application.service.SecuenciaService;
import com.sigo.programacion.application.service.TurnoService;
import com.sigo.programacion.application.service.UbicacionService;
import com.sigo.programacion.infrastructure.persistence.entity.AgenteControladorLider;
import com.sigo.programacion.infrastructure.persistence.repository.AgenteControladorLiderRepository;
import com.sigo.programacion.infrastructure.persistence.repository.DistribucionPersonalRepository;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionSecuenciaAgenteRepository;
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

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@Import({
        SecuenciaGestionJpaAdapter.class,
        SecuenciaOrdenJpaAdapter.class,
        LiderGestionJpaAdapter.class,
        UbicacionGestionJpaAdapter.class,
        TurnoGestionJpaAdapter.class,
        DistribucionGestionJpaAdapter.class,
        MiHorarioGestionJpaAdapter.class
})
@Testcontainers(disabledWithoutDocker = true)
class ProgramacionConfiguracionFlujoIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("sigo_programacion_flujo_test")
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
    private SecuenciaGestionJpaAdapter secuenciaAdapter;

    @Autowired
    private SecuenciaOrdenJpaAdapter secuenciaOrdenAdapter;

    @Autowired
    private LiderGestionJpaAdapter liderAdapter;

    @Autowired
    private UbicacionGestionJpaAdapter ubicacionAdapter;

    @Autowired
    private TurnoGestionJpaAdapter turnoAdapter;

    @Autowired
    private DistribucionGestionJpaAdapter distribucionAdapter;

    @Autowired
    private MiHorarioGestionJpaAdapter miHorarioAdapter;

    @Autowired
    private ProgramacionSecuenciaAgenteRepository secuenciaRepository;

    @Autowired
    private AgenteControladorLiderRepository liderRepository;

    @Autowired
    private DistribucionPersonalRepository distribucionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Plaza plaza;
    private Trabajador supervisor;
    private Trabajador controlador1;
    private Trabajador controlador2;
    private Trabajador agente1;
    private Trabajador agente2;

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

        Puesto puestoSupervisor =
                entityManager.persistAndFlush(
                        new Puesto(
                                null,
                                "Supervisor"
                        )
                );

        Puesto puestoControlador =
                entityManager.persistAndFlush(
                        new Puesto(
                                null,
                                "Controlador"
                        )
                );

        Puesto puestoAgente =
                entityManager.persistAndFlush(
                        new Puesto(
                                null,
                                "Agente de Recaudación"
                        )
                );

        supervisor = trabajador(
                7001,
                "Supervisor Programación",
                puestoSupervisor,
                RolSistema.SUPERVISOR
        );

        controlador1 = trabajador(
                7101,
                "Controlador Uno",
                puestoControlador,
                RolSistema.CONTROLADOR
        );

        controlador2 = trabajador(
                7102,
                "Controlador Dos",
                puestoControlador,
                RolSistema.CONTROLADOR
        );

        agente1 = trabajador(
                7201,
                "Agente Uno",
                puestoAgente,
                RolSistema.OPERADOR
        );

        agente2 = trabajador(
                7202,
                "Agente Dos",
                puestoAgente,
                RolSistema.OPERADOR
        );
    }

    @Test
    void asignaYReordenaSecuenciaPersistida() {
        FixedAccessPort access =
                new FixedAccessPort(supervisor.getId());

        SecuenciaService secuenciaService =
                new SecuenciaService(
                        secuenciaAdapter,
                        access
                );

        GuardarOrdenSecuenciaService ordenService =
                new GuardarOrdenSecuenciaService(
                        secuenciaOrdenAdapter,
                        access
                );

        secuenciaService.asignar(
                new AsignarSecuenciaUseCase.Command(
                        agente1.getId(),
                        plaza.getId(),
                        "SECUENCIA_1"
                )
        );

        secuenciaService.asignar(
                new AsignarSecuenciaUseCase.Command(
                        agente2.getId(),
                        plaza.getId(),
                        "SECUENCIA_1"
                )
        );

        var antes = secuenciaService.listar(
                plaza.getId()
        );

        assertEquals(2, antes.size());
        assertEquals(1, antes.get(0).orden());
        assertEquals(2, antes.get(1).orden());

        ordenService.guardar(
                new GuardarOrdenSecuenciaUseCase.Command(
                        plaza.getId(),
                        "SECUENCIA_1",
                        List.of(
                                new GuardarOrdenSecuenciaUseCase.Item(
                                        agente2.getId(),
                                        1
                                ),
                                new GuardarOrdenSecuenciaUseCase.Item(
                                        agente1.getId(),
                                        2
                                )
                        )
                )
        );

        var despues = secuenciaRepository
                .findActivasByPlazaIdAndGrupo(
                        plaza.getId(),
                        com.sigo.programacion.infrastructure.persistence.entity.GrupoProgramacion.SECUENCIA_1
                );

        assertEquals(2, despues.size());
        assertEquals(agente2.getId(), despues.get(0).getAgente().getId());
        assertEquals(1, despues.get(0).getOrden());
        assertEquals(agente1.getId(), despues.get(1).getAgente().getId());
        assertEquals(2, despues.get(1).getOrden());
    }

    @Test
    void reasignarLiderCierraRelacionAnterior() {
        FixedAccessPort access =
                new FixedAccessPort(supervisor.getId());

        LiderService service =
                new LiderService(
                        liderAdapter,
                        access
                );

        LocalDate primeraFecha =
                LocalDate.of(2026, 9, 1);

        service.asignar(
                new AsignarLiderUseCase.Command(
                        agente1.getId(),
                        controlador1.getId(),
                        plaza.getId(),
                        primeraFecha
                )
        );

        LocalDate segundaFecha =
                LocalDate.of(2026, 9, 10);

        var nuevo = service.asignar(
                new AsignarLiderUseCase.Command(
                        agente1.getId(),
                        controlador2.getId(),
                        plaza.getId(),
                        segundaFecha
                )
        );

        assertEquals(
                controlador2.getId(),
                nuevo.controladorId()
        );

        List<AgenteControladorLider> relaciones =
                liderRepository.findAll();

        assertEquals(2, relaciones.size());

        AgenteControladorLider anterior =
                relaciones.stream()
                        .filter(relacion ->
                                relacion.getControlador()
                                        .getId()
                                        .equals(
                                                controlador1.getId()
                                        )
                        )
                        .findFirst()
                        .orElseThrow();

        assertFalse(anterior.getActivo());
        assertEquals(
                segundaFecha.minusDays(1),
                anterior.getFechaFin()
        );

        AgenteControladorLider actual =
                liderRepository
                        .findByAgenteIdAndActivoTrue(
                                agente1.getId()
                        )
                        .orElseThrow();

        assertEquals(
                controlador2.getId(),
                actual.getControlador().getId()
        );
        assertTrue(actual.getActivo());
    }

    @Test
    void turnoUbicacionDistribucionYMiHorarioFuncionanDePuntaAPunta() {
        FixedAccessPort supervisorAccess =
                new FixedAccessPort(supervisor.getId());

        LiderService liderService =
                new LiderService(
                        liderAdapter,
                        supervisorAccess
                );

        liderService.asignar(
                new AsignarLiderUseCase.Command(
                        agente1.getId(),
                        controlador1.getId(),
                        plaza.getId(),
                        LocalDate.of(2026, 9, 1)
                )
        );

        UbicacionService ubicacionService =
                new UbicacionService(
                        ubicacionAdapter,
                        supervisorAccess
                );

        var ubicacion = ubicacionService.crear(
                new UbicacionUseCase.Command(
                        plaza.getId(),
                        "V01",
                        "Vía 01",
                        "VIA",
                        1
                )
        );

        assertNotNull(ubicacion.id());
        assertEquals("V01", ubicacion.codigo());

        TurnoService turnoService =
                new TurnoService(
                        turnoAdapter,
                        supervisorAccess
                );

        LocalDate fecha =
                LocalDate.of(2026, 9, 15);

        var turnos = turnoService.guardar(
                new GuardarTurnosUseCase.Command(
                        plaza.getId(),
                        List.of(
                                new GuardarTurnosUseCase.Item(
                                        agente1.getId(),
                                        fecha,
                                        "A"
                                )
                        )
                )
        );

        Long programacionTurnoId =
                turnos.get(0).programacionId();

        DistribucionService distribucionService =
                new DistribucionService(
                        distribucionAdapter,
                        supervisorAccess
                );

        var distribuciones = distribucionService.guardar(
                new DistribucionUseCase.Command(
                        plaza.getId(),
                        List.of(
                                new DistribucionUseCase.Item(
                                        programacionTurnoId,
                                        ubicacion.id(),
                                        "Asignación de prueba"
                                )
                        )
                )
        );

        assertEquals(1, distribuciones.size());
        assertEquals("V01", distribuciones.get(0).ubicacionCodigo());
        assertEquals(
                1,
                distribucionRepository.count()
        );

        MiHorarioService miHorarioService =
                new MiHorarioService(
                        miHorarioAdapter,
                        new FixedAccessPort(
                                agente1.getId()
                        )
                );

        var horario = miHorarioService.obtener(
                fecha.minusDays(1),
                fecha.plusDays(1)
        );

        assertEquals(agente1.getId(), horario.trabajadorId());
        assertEquals(
                controlador1.getNombreCompleto(),
                horario.lider()
        );
        assertEquals(3, horario.dias().size());

        var diaProgramado = horario.dias()
                .stream()
                .filter(dia -> dia.fecha().equals(fecha))
                .findFirst()
                .orElseThrow();

        assertEquals("A", diaProgramado.estado());
        assertEquals("V01", diaProgramado.ubicacionCodigo());
        assertEquals("Vía 01", diaProgramado.ubicacionNombre());

        assertTrue(
                horario.dias()
                        .stream()
                        .filter(dia -> !dia.fecha().equals(fecha))
                        .allMatch(dia -> dia.estado() == null)
        );
    }

    private Trabajador trabajador(
            int codigo,
            String nombre,
            Puesto puesto,
            RolSistema rol
    ) {
        Trabajador trabajador = new Trabajador();
        trabajador.setCodigo(codigo);
        trabajador.setNombreCompleto(nombre);
        trabajador.setPuesto(puesto);
        trabajador.setPlaza(plaza);
        trabajador.setRolSistema(rol);
        trabajador.setRequiereCambioPassword(false);
        trabajador.setActivo(true);

        return entityManager.persistAndFlush(
                trabajador
        );
    }

    private static class FixedAccessPort
            implements ProgramacionAccessPort {

        private final Long usuarioId;

        private FixedAccessPort(Long usuarioId) {
            this.usuarioId = usuarioId;
        }

        @Override
        public Long requireSupervisorId() {
            return usuarioId;
        }

        @Override
        public void validarLecturaPlaza(Long plazaId) {
        }

        @Override
        public void validarGestionPlaza(Long plazaId) {
        }

        @Override
        public Long requireGestionPlazaUsuarioId(
                Long plazaId
        ) {
            return usuarioId;
        }

        @Override
        public Long currentUserId() {
            return usuarioId;
        }
    }
}
