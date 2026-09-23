package com.sigo.programacion.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.jwt.secret=01234567890123456789012345678901",
        "app.jwt.issuer=sigo-api",
        "cloudinary.cloud-name=test",
        "cloudinary.api-key=test",
        "cloudinary.api-secret=test",
        "gemini.api-key=test",
        "app.test-bootstrap.enabled=false"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ProgramacionSecuenciaOrdenHttpJpaE2EIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("sigo_programacion_e2e")
                    .withUsername("sigo")
                    .withPassword("sigo");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add(
                "spring.datasource.driver-class-name",
                POSTGRES::getDriverClassName
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private Long plazaId;
    private Long supervisorId;
    private Long agente1Id;
    private Long agente2Id;

    @BeforeEach
    void prepararDatosReales() {
        plazaId = jdbc.queryForObject(
                """
                insert into plazas(codigo, descripcion, activo)
                values ('P4-PROG-E2E', 'Plaza Programación E2E', true)
                returning id
                """,
                Long.class
        );

        Long puestoSupervisorId = jdbc.queryForObject(
                """
                insert into puestos(nombre)
                values ('Supervisor E2E')
                returning id
                """,
                Long.class
        );

        Long puestoAgenteId = jdbc.queryForObject(
                """
                insert into puestos(nombre)
                values ('Agente de Recaudación')
                returning id
                """,
                Long.class
        );

        supervisorId = jdbc.queryForObject(
                """
                insert into trabajadores(
                    codigo,
                    nombre_completo,
                    puesto_id,
                    plaza_id,
                    rol_sistema,
                    requiere_cambio_password,
                    activo
                )
                values (?, ?, ?, null, 'SUPERVISOR', false, true)
                returning id
                """,
                Long.class,
                99100,
                "Supervisor Programación E2E",
                puestoSupervisorId
        );

        agente1Id = jdbc.queryForObject(
                """
                insert into trabajadores(
                    codigo,
                    nombre_completo,
                    puesto_id,
                    plaza_id,
                    rol_sistema,
                    requiere_cambio_password,
                    activo
                )
                values (?, ?, ?, ?, 'OPERADOR', false, true)
                returning id
                """,
                Long.class,
                99101,
                "Agente Uno E2E",
                puestoAgenteId,
                plazaId
        );

        agente2Id = jdbc.queryForObject(
                """
                insert into trabajadores(
                    codigo,
                    nombre_completo,
                    puesto_id,
                    plaza_id,
                    rol_sistema,
                    requiere_cambio_password,
                    activo
                )
                values (?, ?, ?, ?, 'OPERADOR', false, true)
                returning id
                """,
                Long.class,
                99102,
                "Agente Dos E2E",
                puestoAgenteId,
                plazaId
        );

        jdbc.update(
                """
                insert into programacion_secuencia_agente(
                    agente_id,
                    plaza_id,
                    grupo,
                    orden,
                    actualizado_por
                )
                values (?, ?, 'SECUENCIA_1', 1, ?)
                """,
                agente1Id,
                plazaId,
                supervisorId
        );

        jdbc.update(
                """
                insert into programacion_secuencia_agente(
                    agente_id,
                    plaza_id,
                    grupo,
                    orden,
                    actualizado_por
                )
                values (?, ?, 'SECUENCIA_1', 2, ?)
                """,
                agente2Id,
                plazaId,
                supervisorId
        );
    }

    @Test
    void reordenarSecuenciaNoDevuelve500YPersisteNuevoOrden()
            throws Exception {

        mockMvc.perform(
                        put("/api/programacion/secuencias/orden")
                                .with(jwtSupervisor())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "plazaId": %d,
                                          "grupo": "SECUENCIA_1",
                                          "agentes": [
                                            {
                                              "agenteId": %d,
                                              "orden": 1
                                            },
                                            {
                                              "agenteId": %d,
                                              "orden": 2
                                            }
                                          ]
                                        }
                                        """.formatted(
                                                plazaId,
                                                agente2Id,
                                                agente1Id
                                        )
                                )
                )
                .andExpect(status().isNoContent());

        List<Map<String, Object>> filas = jdbc.queryForList(
                """
                select agente_id, orden, actualizado_por
                from programacion_secuencia_agente
                where plaza_id = ?
                  and grupo = 'SECUENCIA_1'
                order by orden
                """,
                plazaId
        );

        assertEquals(2, filas.size());

        assertEquals(
                agente2Id.longValue(),
                ((Number) filas.get(0).get("agente_id"))
                        .longValue()
        );
        assertEquals(
                1,
                ((Number) filas.get(0).get("orden"))
                        .intValue()
        );

        assertEquals(
                agente1Id.longValue(),
                ((Number) filas.get(1).get("agente_id"))
                        .longValue()
        );
        assertEquals(
                2,
                ((Number) filas.get(1).get("orden"))
                        .intValue()
        );

        for (Map<String, Object> fila : filas) {
            Number actualizadoPor =
                    (Number) fila.get("actualizado_por");

            assertNotNull(actualizadoPor);
            assertEquals(
                    supervisorId.longValue(),
                    actualizadoPor.longValue()
            );
        }
    }

    private RequestPostProcessor jwtSupervisor() {
        return jwt()
                .jwt(token ->
                        token
                                .subject("99100")
                                .claim("rol", "SUPERVISOR")
                )
                .authorities(
                        new SimpleGrantedAuthority(
                                "ROLE_SUPERVISOR"
                        )
                );
    }
}
