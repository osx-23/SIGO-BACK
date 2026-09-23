package com.sigo.relevo.e2e;

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
class RelevoHttpJpaE2EIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("sigo_relevo_e2e")
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
    private Long turnoId;
    private Long operadorId;
    private Long banosId;
    private Long conosId;
    private Long viaId;

    @BeforeEach
    void prepararDatosReales() {
        plazaId = jdbc.queryForObject(
                """
                insert into plazas(codigo, descripcion, activo)
                values ('P4-REL-E2E', 'Plaza Relevo E2E', true)
                returning id
                """,
                Long.class
        );

        Long puestoId = jdbc.queryForObject(
                """
                insert into puestos(nombre)
                values ('Agente de Recaudación E2E')
                returning id
                """,
                Long.class
        );

        turnoId = jdbc.queryForObject(
                """
                insert into turnos(codigo, nombre)
                values ('B', 'Turno B E2E')
                returning id
                """,
                Long.class
        );

        operadorId = jdbc.queryForObject(
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
                99601,
                "Operador Relevo E2E",
                puestoId,
                plazaId
        );

        banosId = jdbc.queryForObject(
                """
                insert into elementos_relevo(
                    codigo, nombre, categoria,
                    requiere_cantidad, activo, orden
                )
                values ('BANOS-E2E', 'Baños E2E', 'BASE', false, true, 1)
                returning id
                """,
                Long.class
        );

        conosId = jdbc.queryForObject(
                """
                insert into elementos_relevo(
                    codigo, nombre, categoria,
                    requiere_cantidad, activo, orden
                )
                values ('CONOS-E2E', 'Conos E2E', 'PLAZA', true, true, 2)
                returning id
                """,
                Long.class
        );

        viaId = jdbc.queryForObject(
                """
                insert into vias(
                    plaza_id, numero, nombre, activa, orden
                )
                values (?, 1, 'Vía 1 E2E', true, 1)
                returning id
                """,
                Long.class,
                plazaId
        );
    }

    @Test
    void jwtHttpAplicacionJpaYPostgresRegistranRelevoCompleto()
            throws Exception {

        mockMvc.perform(
                        post("/api/relevos")
                                .with(jwtOperador())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "plazaId": 999999,
                                          "turnoId": %d,
                                          "operadorId": 999999,
                                          "fecha": "2026-09-23",
                                          "hora": "06:15:00",
                                          "observaciones": "Sin novedades",
                                          "resumen": "Relevo E2E",
                                          "checklist": [
                                            {
                                              "elementoId": %d,
                                              "estado": "OPERATIVO"
                                            },
                                            {
                                              "elementoId": %d,
                                              "estado": "OPERATIVO",
                                              "cantidad": 10
                                            }
                                          ],
                                          "vias": [
                                            {
                                              "viaId": %d,
                                              "estado": "OPERATIVO"
                                            }
                                          ]
                                        }
                                        """.formatted(
                                                turnoId,
                                                banosId,
                                                conosId,
                                                viaId
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.plazaId").value(plazaId))
                .andExpect(jsonPath("$.operadorId").value(operadorId))
                .andExpect(jsonPath("$.checklist.length()").value(2))
                .andExpect(jsonPath("$.vias.length()").value(1));

        Integer relevos = jdbc.queryForObject(
                """
                select count(*)
                from relevos
                where plaza_id = ?
                  and turno_id = ?
                  and operador_id = ?
                  and fecha = date '2026-09-23'
                """,
                Integer.class,
                plazaId,
                turnoId,
                operadorId
        );

        Integer checklist = jdbc.queryForObject(
                "select count(*) from relevo_checklist",
                Integer.class
        );

        Integer vias = jdbc.queryForObject(
                "select count(*) from relevo_vias",
                Integer.class
        );

        assertNotNull(relevos);
        assertEquals(1, relevos);
        assertEquals(2, checklist);
        assertEquals(1, vias);
    }

    private RequestPostProcessor jwtOperador() {
        return jwt()
                .jwt(token ->
                        token
                                .subject("99601")
                                .claim("rol", "OPERADOR")
                )
                .authorities(
                        new SimpleGrantedAuthority(
                                "ROLE_OPERADOR"
                        )
                );
    }
}
