package com.sigo.asistencia.infrastructure.persistence.adapter;

import com.sigo.asistencia.application.port.in.GestionarAsistenciaUseCase;
import com.sigo.asistencia.application.port.in.GestionarEvidenciaAsistenciaUseCase;
import com.sigo.asistencia.application.port.out.AsistenciaStoragePort;
import com.sigo.asistencia.application.service.AsistenciaConsultaService;
import com.sigo.asistencia.application.service.DashboardAsistenciaService;
import com.sigo.asistencia.application.service.GestionarAsistenciaService;
import com.sigo.asistencia.application.service.GestionarEvidenciaAsistenciaService;
import com.sigo.asistencia.infrastructure.persistence.entity.MotivoAusencia;
import com.sigo.asistencia.infrastructure.persistence.repository.AsistenciaAusenciaRepository;
import com.sigo.asistencia.infrastructure.persistence.repository.AsistenciaEvidenciaRepository;
import com.sigo.asistencia.infrastructure.persistence.repository.AsistenciaRepository;
import com.sigo.asistencia.infrastructure.persistence.repository.MotivoAusenciaRepository;
import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Puesto;
import com.sigo.personal.infrastructure.persistence.entity.RolSistema;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.entity.Turno;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.personal.infrastructure.persistence.repository.TurnoRepository;
import com.sigo.shared.exception.BusinessException;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@Import({
        AsistenciaGestionJpaAdapter.class,
        AsistenciaConsultaJpaAdapter.class,
        AsistenciaEvidenciaJpaAdapter.class,
        DashboardAsistenciaJpaAdapter.class
})
@Testcontainers(disabledWithoutDocker = true)
class AsistenciaFlujoJpaIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("sigo_asistencia_flujo_test")
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
    private AsistenciaGestionJpaAdapter gestionAdapter;

    @Autowired
    private AsistenciaConsultaJpaAdapter consultaAdapter;

    @Autowired
    private AsistenciaEvidenciaJpaAdapter evidenciaAdapter;

    @Autowired
    private DashboardAsistenciaJpaAdapter dashboardAdapter;

    @Autowired
    private PlazaRepository plazaRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private TrabajadorRepository trabajadorRepository;

    @Autowired
    private MotivoAusenciaRepository motivoRepository;

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private AsistenciaAusenciaRepository ausenciaRepository;

    @Autowired
    private AsistenciaEvidenciaRepository evidenciaRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Plaza plaza;
    private Turno turnoA;
    private Turno turnoB;
    private Trabajador controlador;
    private Trabajador agente1;
    private Trabajador agente2;
    private MotivoAusencia descansoMedico;
    private MotivoAusencia faltaInjustificada;

    private AsistenciaConsultaService consultaService;
    private GestionarAsistenciaService gestionService;
    private DashboardAsistenciaService dashboardService;

    @BeforeEach
    void prepararDatos() {
        plaza = plazaRepository.saveAndFlush(
                new Plaza(null, "P4", "Plaza 4", true)
        );

        turnoA = turnoRepository.saveAndFlush(
                new Turno(null, "A", "Turno A")
        );

        turnoB = turnoRepository.saveAndFlush(
                new Turno(null, "B", "Turno B")
        );

        Puesto puestoControlador = entityManager.persistAndFlush(
                new Puesto(null, "Controlador")
        );

        Puesto puestoAgente = entityManager.persistAndFlush(
                new Puesto(null, "Agente de Recaudación")
        );

        controlador = trabajador(
                9601,
                "Controlador Asistencia",
                puestoControlador,
                RolSistema.CONTROLADOR
        );

        agente1 = trabajador(
                9602,
                "Agente Uno",
                puestoAgente,
                RolSistema.OPERADOR
        );

        agente2 = trabajador(
                9603,
                "Agente Dos",
                puestoAgente,
                RolSistema.OPERADOR
        );

        descansoMedico = motivoRepository.saveAndFlush(
                new MotivoAusencia(null, "Descanso Médico")
        );

        faltaInjustificada = motivoRepository.saveAndFlush(
                new MotivoAusencia(null, "Falta Injustificada")
        );

        consultaService =
                new AsistenciaConsultaService(consultaAdapter);

        gestionService =
                new GestionarAsistenciaService(
                        gestionAdapter,
                        consultaService
                );

        dashboardService =
                new DashboardAsistenciaService(
                        dashboardAdapter
                );
    }

    @Test
    void flujoCompletoRegistroAusenciaEvidenciaActualizacionHistorialYDashboard()
            throws IOException {
        LocalDate fecha = LocalDate.of(2026, 9, 23);

        var registrada = gestionService.registrar(
                command(
                        turnoA,
                        fecha,
                        7,
                        6,
                        1,
                        "Necesita apoyo en hora punta",
                        List.of(
                                new GestionarAsistenciaUseCase.AusenciaCommand(
                                        agente1.getId(),
                                        descansoMedico.getId(),
                                        "Reposo"
                                )
                        ),
                        List.of()
                )
        );

        assertNotNull(registrada.id());
        assertEquals(1, registrada.ausencias().size());
        assertEquals(1, registrada.ausentes());
        assertEquals(7, registrada.programados());
        assertEquals(6, registrada.presentes());

        StubStorage storage = new StubStorage();

        GestionarEvidenciaAsistenciaService evidenciaService =
                new GestionarEvidenciaAsistenciaService(
                        evidenciaAdapter,
                        storage
                );

        var evidencia = evidenciaService.guardar(
                registrada.id(),
                archivo("inicio.jpg"),
                " inicio_turno "
        );

        assertNotNull(evidencia.id());
        assertEquals("INICIO_TURNO", evidencia.tipo());

        var conEvidencia =
                consultaService.obtenerPorId(registrada.id());

        assertEquals(1, conEvidencia.evidencias().size());
        assertEquals(
                "INICIO_TURNO",
                conEvidencia.evidencias().get(0).tipo()
        );

        var actualizada = gestionService.actualizar(
                registrada.id(),
                command(
                        turnoA,
                        fecha,
                        7,
                        5,
                        0,
                        "debe borrarse",
                        List.of(
                                new GestionarAsistenciaUseCase.AusenciaCommand(
                                        agente1.getId(),
                                        descansoMedico.getId(),
                                        "Continúa de reposo"
                                ),
                                new GestionarAsistenciaUseCase.AusenciaCommand(
                                        agente2.getId(),
                                        faltaInjustificada.getId(),
                                        "No se presentó"
                                )
                        ),
                        List.of()
                )
        );

        assertEquals(registrada.id(), actualizada.id());
        assertEquals(2, actualizada.ausencias().size());
        assertEquals(2, actualizada.ausentes());
        assertNull(actualizada.detalleApoyo());

        assertEquals(
                2,
                ausenciaRepository
                        .findByAsistenciaId(registrada.id())
                        .size()
        );

        // Las evidencias se administran por su caso de uso separado;
        // una actualización de datos no debe eliminarlas.
        assertEquals(
                1,
                evidenciaRepository
                        .findByAsistenciaId(registrada.id())
                        .size()
        );

        var historial = consultaService.listar(
                fecha,
                fecha,
                plaza.getId()
        );

        assertEquals(1, historial.size());
        assertEquals(registrada.id(), historial.get(0).id());
        assertEquals(2, historial.get(0).ausencias().size());
        assertEquals(1, historial.get(0).evidencias().size());

        var resumen = dashboardService.resumen(
                fecha,
                fecha,
                plaza.getId(),
                turnoA.getId()
        );

        assertEquals(1L, resumen.registros());
        assertEquals(5L, resumen.presentes());
        assertEquals(7L, resumen.programados());
        assertEquals(2L, resumen.ausentes());
        assertEquals(
                0,
                resumen.porcentajeGeneral()
                        .compareTo(new BigDecimal("71.43"))
        );

        var diario = dashboardService.diario(
                2026,
                9,
                plaza.getId(),
                turnoA.getId()
        );

        assertEquals(1, diario.size());
        assertEquals(23, diario.get(0).periodo());
        assertEquals(5L, diario.get(0).presentes());
        assertEquals(7L, diario.get(0).programados());

        var motivos = dashboardService.motivos(
                2026,
                9,
                plaza.getId(),
                turnoA.getId()
        );

        assertEquals(2, motivos.size());
        assertTrue(
                motivos.stream().anyMatch(item ->
                        item.motivo().equals("Descanso Médico")
                                && item.total() == 1L
                )
        );
        assertTrue(
                motivos.stream().anyMatch(item ->
                        item.motivo().equals("Falta Injustificada")
                                && item.total() == 1L
                )
        );

        evidenciaService.eliminar(
                registrada.id(),
                evidencia.id()
        );

        assertEquals(
                0,
                evidenciaRepository
                        .findByAsistenciaId(registrada.id())
                        .size()
        );
        assertEquals(1, storage.eliminados);
    }

    @Test
    void rechazaDuplicadoMismaPlazaTurnoYFecha() {
        LocalDate fecha = LocalDate.of(2026, 9, 24);

        gestionService.registrar(
                command(
                        turnoA,
                        fecha,
                        7,
                        7,
                        0,
                        null,
                        List.of(),
                        List.of()
                )
        );

        assertThrows(
                BusinessException.class,
                () -> gestionService.registrar(
                        command(
                                turnoA,
                                fecha,
                                7,
                                7,
                                0,
                                null,
                                List.of(),
                                List.of()
                        )
                )
        );

        assertEquals(1, asistenciaRepository.count());
    }

    @Test
    void permiteMismaFechaEnOtroTurno() {
        LocalDate fecha = LocalDate.of(2026, 9, 25);

        gestionService.registrar(
                command(
                        turnoA,
                        fecha,
                        7,
                        7,
                        0,
                        null,
                        List.of(),
                        List.of()
                )
        );

        gestionService.registrar(
                command(
                        turnoB,
                        fecha,
                        7,
                        7,
                        0,
                        null,
                        List.of(),
                        List.of()
                )
        );

        assertEquals(2, asistenciaRepository.count());
    }

    @Test
    void loteAusenciasInvalidoNoPersisteRegistro() {
        assertThrows(
                BusinessException.class,
                () -> gestionService.registrar(
                        command(
                                turnoA,
                                LocalDate.of(2026, 9, 26),
                                7,
                                5,
                                0,
                                null,
                                List.of(
                                        new GestionarAsistenciaUseCase.AusenciaCommand(
                                                agente1.getId(),
                                                descansoMedico.getId(),
                                                "Solo una ausencia"
                                        )
                                ),
                                List.of()
                        )
                )
        );

        assertEquals(0, asistenciaRepository.count());
        assertEquals(0, ausenciaRepository.count());
    }

    @Test
    void evidenciaConTipoInvalidoNoSeGuardaNiSube()
            throws IOException {
        var registrada = gestionService.registrar(
                command(
                        turnoA,
                        LocalDate.of(2026, 9, 27),
                        7,
                        7,
                        0,
                        null,
                        List.of(),
                        List.of()
                )
        );

        StubStorage storage = new StubStorage();

        GestionarEvidenciaAsistenciaService service =
                new GestionarEvidenciaAsistenciaService(
                        evidenciaAdapter,
                        storage
                );

        assertThrows(
                BusinessException.class,
                () -> service.guardar(
                        registrada.id(),
                        archivo("x.jpg"),
                        "OTRO"
                )
        );

        assertEquals(0, storage.subidos);
        assertEquals(0, evidenciaRepository.count());
    }

    private GestionarAsistenciaUseCase.Command command(
            Turno turno,
            LocalDate fecha,
            int programados,
            int presentes,
            int apoyo,
            String detalleApoyo,
            List<GestionarAsistenciaUseCase.AusenciaCommand> ausencias,
            List<String> evidencias
    ) {
        return new GestionarAsistenciaUseCase.Command(
                plaza.getId(),
                turno.getId(),
                controlador.getId(),
                fecha,
                programados,
                presentes,
                apoyo,
                detalleApoyo,
                "  Nota de integración  ",
                ausencias,
                evidencias
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
        return trabajadorRepository.saveAndFlush(trabajador);
    }

    private GestionarEvidenciaAsistenciaUseCase.ArchivoEntrada archivo(
            String nombre
    ) {
        return new GestionarEvidenciaAsistenciaUseCase.ArchivoEntrada(
                nombre,
                "image/jpeg",
                new byte[]{1, 2, 3}
        );
    }

    private static class StubStorage
            implements AsistenciaStoragePort {

        private int subidos;
        private int eliminados;

        @Override
        public ArchivoSubido subir(
                GestionarEvidenciaAsistenciaUseCase.ArchivoEntrada archivo,
                String folder
        ) {
            subidos++;
            return new ArchivoSubido(
                    "https://storage.test/" + subidos + ".jpg",
                    "asistencia-" + subidos
            );
        }

        @Override
        public void eliminar(String publicId) {
            eliminados++;
        }
    }
}
