package com.sigo.programacion.infrastructure.persistence.adapter;

import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Puesto;
import com.sigo.personal.infrastructure.persistence.entity.RolSistema;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.programacion.application.port.in.GuardarTurnosUseCase;
import com.sigo.programacion.application.port.out.ProgramacionAccessPort;
import com.sigo.programacion.application.service.TurnoService;
import com.sigo.programacion.infrastructure.persistence.entity.EstadoProgramacion;
import com.sigo.programacion.infrastructure.persistence.entity.ProgramacionTurno;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionTurnoRepository;
import com.sigo.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
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
@Import(TurnoGestionJpaAdapter.class)
@Testcontainers(disabledWithoutDocker = true)
class TurnoGestionJpaAdapterIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("sigo_turnos_test")
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
    private TurnoGestionJpaAdapter turnoGestionPort;

    @Autowired
    private ProgramacionTurnoRepository programacionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Plaza plaza;
    private Plaza otraPlaza;
    private Trabajador supervisor;
    private Trabajador agente;
    private Trabajador agenteOtraPlaza;
    private TurnoService service;

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

        otraPlaza = entityManager.persistAndFlush(
                new Plaza(
                        null,
                        "P5",
                        "Plaza 5",
                        true
                )
        );

        Puesto puestoSupervisor = entityManager.persistAndFlush(
                new Puesto(
                        null,
                        "Supervisor"
                )
        );

        Puesto puestoAgente = entityManager.persistAndFlush(
                new Puesto(
                        null,
                        "Agente de Recaudación"
                )
        );

        supervisor = trabajador(
                8001,
                "Supervisor Test",
                puestoSupervisor,
                plaza,
                RolSistema.SUPERVISOR
        );

        agente = trabajador(
                8101,
                "Agente P4",
                puestoAgente,
                plaza,
                RolSistema.OPERADOR
        );

        agenteOtraPlaza = trabajador(
                8201,
                "Agente P5",
                puestoAgente,
                otraPlaza,
                RolSistema.OPERADOR
        );

        service = new TurnoService(
                turnoGestionPort,
                new SupervisorAccessPort(
                        supervisor.getId()
                )
        );
    }

    @Test
    void guardaYVuelveAConsultarProgramacionPersistida() {
        LocalDate fecha = LocalDate.of(2026, 9, 1);

        var guardados = service.guardar(
                command(
                        new GuardarTurnosUseCase.Item(
                                agente.getId(),
                                fecha,
                                "A"
                        )
                )
        );

        assertEquals(1, guardados.size());
        assertNotNull(guardados.get(0).programacionId());
        assertEquals("A", guardados.get(0).estado());

        var listado = service.listar(
                plaza.getId(),
                2026,
                9
        );

        assertEquals(1, listado.size());
        assertEquals(
                guardados.get(0).programacionId(),
                listado.get(0).programacionId()
        );
        assertEquals(fecha, listado.get(0).fecha());
        assertEquals("A", listado.get(0).estado());
    }

    @Test
    void actualizarTurnoExistenteConservaMismoRegistro() {
        LocalDate fecha = LocalDate.of(2026, 9, 2);

        var inicial = service.guardar(
                command(
                        new GuardarTurnosUseCase.Item(
                                agente.getId(),
                                fecha,
                                "A"
                        )
                )
        );

        Long idInicial =
                inicial.get(0).programacionId();

        var actualizado = service.guardar(
                command(
                        new GuardarTurnosUseCase.Item(
                                agente.getId(),
                                fecha,
                                "B"
                        )
                )
        );

        assertEquals(1, actualizado.size());
        assertEquals(
                idInicial,
                actualizado.get(0).programacionId()
        );
        assertEquals("B", actualizado.get(0).estado());

        assertEquals(
                1,
                programacionRepository.count()
        );

        ProgramacionTurno persistido =
                programacionRepository
                        .findByTrabajadorIdAndFecha(
                                agente.getId(),
                                fecha
                        )
                        .orElseThrow();

        assertEquals(
                EstadoProgramacion.B,
                persistido.getEstado()
        );
    }

    @Test
    void loteConEstadoInvalidoNoDejaRegistrosParciales() {
        LocalDate fecha1 = LocalDate.of(2026, 9, 3);
        LocalDate fecha2 = LocalDate.of(2026, 9, 4);

        var command =
                new GuardarTurnosUseCase.Command(
                        plaza.getId(),
                        List.of(
                                new GuardarTurnosUseCase.Item(
                                        agente.getId(),
                                        fecha1,
                                        "A"
                                ),
                                new GuardarTurnosUseCase.Item(
                                        agente.getId(),
                                        fecha2,
                                        "INVALIDO"
                                )
                        )
                );

        assertThrows(
                BusinessException.class,
                () -> service.guardar(command)
        );

        assertEquals(
                0,
                programacionRepository.count()
        );
    }

    @Test
    void rechazaAgenteDeOtraPlazaSinPersistirNada() {
        var command =
                command(
                        new GuardarTurnosUseCase.Item(
                                agenteOtraPlaza.getId(),
                                LocalDate.of(2026, 9, 5),
                                "A"
                        )
                );

        assertThrows(
                BusinessException.class,
                () -> service.guardar(command)
        );

        assertEquals(
                0,
                programacionRepository.count()
        );
    }

    @Test
    void postgresImpideDosFilasParaMismoTrabajadorYFecha() {
        LocalDate fecha = LocalDate.of(2026, 9, 6);

        ProgramacionTurno primero = nuevaProgramacion(
                agente,
                fecha,
                EstadoProgramacion.A
        );

        programacionRepository.saveAndFlush(primero);

        ProgramacionTurno duplicado = nuevaProgramacion(
                agente,
                fecha,
                EstadoProgramacion.B
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> programacionRepository.saveAndFlush(
                        duplicado
                )
        );
    }

    @Test
    void mismoTrabajadorPuedeTenerTurnosEnFechasDiferentes() {
        service.guardar(
                new GuardarTurnosUseCase.Command(
                        plaza.getId(),
                        List.of(
                                new GuardarTurnosUseCase.Item(
                                        agente.getId(),
                                        LocalDate.of(2026, 9, 7),
                                        "A"
                                ),
                                new GuardarTurnosUseCase.Item(
                                        agente.getId(),
                                        LocalDate.of(2026, 9, 8),
                                        "D"
                                )
                        )
                )
        );

        assertEquals(
                2,
                programacionRepository.count()
        );
    }

    private GuardarTurnosUseCase.Command command(
            GuardarTurnosUseCase.Item item
    ) {
        return new GuardarTurnosUseCase.Command(
                plaza.getId(),
                List.of(item)
        );
    }

    private Trabajador trabajador(
            int codigo,
            String nombre,
            Puesto puesto,
            Plaza plazaTrabajador,
            RolSistema rol
    ) {
        Trabajador trabajador = new Trabajador();
        trabajador.setCodigo(codigo);
        trabajador.setNombreCompleto(nombre);
        trabajador.setPuesto(puesto);
        trabajador.setPlaza(plazaTrabajador);
        trabajador.setRolSistema(rol);
        trabajador.setRequiereCambioPassword(false);
        trabajador.setActivo(true);

        return entityManager.persistAndFlush(
                trabajador
        );
    }

    private ProgramacionTurno nuevaProgramacion(
            Trabajador trabajador,
            LocalDate fecha,
            EstadoProgramacion estado
    ) {
        ProgramacionTurno programacion =
                new ProgramacionTurno();

        programacion.setTrabajador(trabajador);
        programacion.setPlaza(plaza);
        programacion.setFecha(fecha);
        programacion.setEstado(estado);
        programacion.setCreadoPor(supervisor);
        programacion.setActualizadoPor(supervisor);

        return programacion;
    }

    private static class SupervisorAccessPort
            implements ProgramacionAccessPort {

        private final Long supervisorId;

        private SupervisorAccessPort(Long supervisorId) {
            this.supervisorId = supervisorId;
        }

        @Override
        public Long requireSupervisorId() {
            return supervisorId;
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
            return supervisorId;
        }

        @Override
        public Long currentUserId() {
            return supervisorId;
        }
    }
}
