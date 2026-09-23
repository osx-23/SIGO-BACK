package com.sigo.asistencia.e2e;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class AsistenciaHttpJpaE2EIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("sigo_asistencia_e2e")
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
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private Long plazaId;
    private Long turnoId;
    private Long controladorId;

    @BeforeEach
    void prepararDatosReales() {
        plazaId = jdbc.queryForObject(
                """
                insert into plazas(codigo, descripcion, activo)
                values ('P4-AS-E2E', 'Plaza Asistencia E2E', true)
                returning id
                """,
                Long.class
        );

        Long puestoId = jdbc.queryForObject(
                """
                insert into puestos(nombre)
                values ('Controlador')
                returning id
                """,
                Long.class
        );

        turnoId = jdbc.queryForObject(
                """
                insert into turnos(codigo, nombre)
                values ('A', 'Turno A E2E')
                returning id
                """,
                Long.class
        );

        controladorId = jdbc.queryForObject(
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
                values (?, ?, ?, ?, 'CONTROLADOR', false, true)
                returning id
                """,
                Long.class,
                99550,
                "Controlador Asistencia E2E",
                puestoId,
                plazaId
        );
    }

    @Test
    void jwtHttpAplicacionJpaYPostgresRegistranAsistencia()
            throws Exception {

        mockMvc.perform(
                        post("/api/asistencias")
                                .with(jwtControlador())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "plazaId": %d,
                                          "turnoId": %d,
                                          "controladorId": %d,
                                          "fecha": "2026-09-23",
                                          "programados": 7,
                                          "presentes": 7,
                                          "apoyoSolicitado": 0,
                                          "ausencias": [],
                                          "evidencias": []
                                        }
                                        """.formatted(
                                                plazaId,
                                                turnoId,
                                                controladorId
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(
                        jsonPath("$.plazaId")
                                .value(plazaId)
                )
                .andExpect(
                        jsonPath("$.turnoId")
                                .value(turnoId)
                )
                .andExpect(
                        jsonPath("$.controladorId")
                                .value(controladorId)
                )
                .andExpect(
                        jsonPath("$.programados")
                                .value(7)
                )
                .andExpect(
                        jsonPath("$.presentes")
                                .value(7)
                );

        Integer total = jdbc.queryForObject(
                """
                select count(*)
                from asistencia_registro
                where plaza_id = ?
                  and turno_id = ?
                  and controlador_id = ?
                  and fecha = date '2026-09-23'
                """,
                Integer.class,
                plazaId,
                turnoId,
                controladorId
        );

        assertNotNull(total);
        assertEquals(1, total);
    }

    private RequestPostProcessor jwtControlador() {
        return jwt()
                .jwt(token ->
                        token
                                .subject("99550")
                                .claim("rol", "CONTROLADOR")
                )
                .authorities(
                        new SimpleGrantedAuthority(
                                "ROLE_CONTROLADOR"
                        )
                );
    }
}
