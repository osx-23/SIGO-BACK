package com.sigo.asistencia.infrastructure.persistence.adapter;

import com.sigo.asistencia.application.port.in.GestionarAsistenciaUseCase;
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
import com.sigo.personal.infrastructure.persistence.repository.PuestoRepository;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.personal.infrastructure.persistence.repository.TurnoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@Import(AsistenciaGestionJpaAdapter.class)
@Testcontainers(disabledWithoutDocker = true)
class AsistenciaGestionJpaAdapterIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("sigo_test")
                    .withUsername("sigo")
                    .withPassword("sigo");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
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
    private AsistenciaGestionJpaAdapter adapter;

    @Autowired
    private PlazaRepository plazaRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private PuestoRepository puestoRepository;

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

    @Test
    void registraAsistenciaConAusenciaYEvidenciaInicial() {
        Plaza plaza = new Plaza(
                null,
                "P4",
                "Plaza 4",
                true
        );
        plaza = plazaRepository.saveAndFlush(plaza);

        Turno turno = new Turno(
                null,
                "A",
                "Turno A"
        );
        turno = turnoRepository.saveAndFlush(turno);

        Puesto puestoControlador = new Puesto(
                null,
                "Controlador"
        );
        puestoControlador =
                puestoRepository.saveAndFlush(
                        puestoControlador
                );

        Puesto puestoAgente = new Puesto(
                null,
                "Agente de Recaudación"
        );
        puestoAgente =
                puestoRepository.saveAndFlush(
                        puestoAgente
                );

        Trabajador controlador = new Trabajador();
        controlador.setCodigo(287);
        controlador.setNombreCompleto("Controlador Test");
        controlador.setPuesto(puestoControlador);
        controlador.setPlaza(plaza);
        controlador.setRolSistema(RolSistema.CONTROLADOR);
        controlador.setRequiereCambioPassword(false);
        controlador.setActivo(true);
        controlador =
                trabajadorRepository.saveAndFlush(
                        controlador
                );

        Trabajador agente = new Trabajador();
        agente.setCodigo(9001);
        agente.setNombreCompleto("Agente Test");
        agente.setPuesto(puestoAgente);
        agente.setPlaza(plaza);
        agente.setRolSistema(RolSistema.OPERADOR);
        agente.setRequiereCambioPassword(false);
        agente.setActivo(true);
        agente =
                trabajadorRepository.saveAndFlush(
                        agente
                );

        MotivoAusencia motivo =
                new MotivoAusencia(
                        null,
                        "Descanso Médico"
                );
        motivo = motivoRepository.saveAndFlush(motivo);

        Long asistenciaId = adapter.registrar(
                new GestionarAsistenciaUseCase.Command(
                        plaza.getId(),
                        turno.getId(),
                        controlador.getId(),
                        LocalDate.of(2026, 9, 23),
                        7,
                        6,
                        0,
                        null,
                        "Registro de integración",
                        List.of(
                                new GestionarAsistenciaUseCase.AusenciaCommand(
                                        agente.getId(),
                                        motivo.getId(),
                                        "Reposo"
                                )
                        ),
                        List.of(
                                "https://example.test/evidencia.jpg"
                        )
                )
        );

        assertTrue(
                asistenciaRepository.findById(
                        asistenciaId
                ).isPresent()
        );

        assertEquals(
                1,
                ausenciaRepository
                        .findByAsistenciaId(asistenciaId)
                        .size()
        );

        assertEquals(
                1,
                evidenciaRepository
                        .findByAsistenciaId(asistenciaId)
                        .size()
        );
    }
}
