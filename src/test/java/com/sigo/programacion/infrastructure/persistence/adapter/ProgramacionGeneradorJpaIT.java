package com.sigo.programacion.infrastructure.persistence.adapter;

import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Puesto;
import com.sigo.personal.infrastructure.persistence.entity.RolSistema;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.programacion.application.port.in.GenerarProgramacionUseCase;
import com.sigo.programacion.application.port.out.ProgramacionAccessPort;
import com.sigo.programacion.application.service.ProgramacionGeneradorService;
import com.sigo.programacion.domain.ProgramacionEstado;
import com.sigo.programacion.infrastructure.persistence.entity.GrupoProgramacion;
import com.sigo.programacion.infrastructure.persistence.entity.ProgramacionSecuenciaAgente;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionSecuenciaAgenteRepository;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@Import(ProgramacionGeneradorJpaAdapter.class)
@Testcontainers(disabledWithoutDocker = true)
class ProgramacionGeneradorJpaIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("sigo_programacion_test")
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
    private ProgramacionGeneradorJpaAdapter dataPort;

    @Autowired
    private ProgramacionSecuenciaAgenteRepository secuenciaRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void generaPropuesta6x2UsandoConfiguracionPersistida() {
        Plaza plaza = entityManager.persistAndFlush(
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

        Trabajador agente = new Trabajador();
        agente.setCodigo(9101);
        agente.setNombreCompleto(
                "Agente Integración Programación"
        );
        agente.setPuesto(puesto);
        agente.setPlaza(plaza);
        agente.setRolSistema(RolSistema.OPERADOR);
        agente.setRequiereCambioPassword(false);
        agente.setActivo(true);

        agente = entityManager.persistAndFlush(agente);

        ProgramacionSecuenciaAgente secuencia =
                new ProgramacionSecuenciaAgente();

        secuencia.setAgente(agente);
        secuencia.setPlaza(plaza);
        secuencia.setGrupo(
                GrupoProgramacion.SECUENCIA_1
        );
        secuencia.setOrden(1);

        secuenciaRepository.saveAndFlush(secuencia);

        ProgramacionGeneradorService service =
                new ProgramacionGeneradorService(
                        dataPort,
                        new SupervisorPermitido()
                );

        var resultado = service.generar(
                new GenerarProgramacionUseCase.GenerarProgramacionRequest(
                        plaza.getId(),
                        2026,
                        9,
                        new GenerarProgramacionUseCase.CoberturaTurnosRequest(
                                1,
                                1,
                                1
                        ),
                        null,
                        List.of(),
                        List.of()
                )
        );

        assertEquals("P4", resultado.plazaCodigo());
        assertEquals(1, resultado.agentes().size());

        var dias = resultado.agentes().get(0).dias();

        assertEquals(30, dias.size());

        long descansosPrimerBloque =
                dias.subList(0, 8)
                        .stream()
                        .filter(dia ->
                                dia.estado()
                                        == ProgramacionEstado.D
                        )
                        .count();

        assertEquals(2, descansosPrimerBloque);

        assertFalse(resultado.cobertura().isEmpty());
    }

    private static class SupervisorPermitido
            implements ProgramacionAccessPort {

        @Override
        public Long requireSupervisorId() {
            return 1L;
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
            return 1L;
        }

        @Override
        public Long currentUserId() {
            return 1L;
        }
    }
}
