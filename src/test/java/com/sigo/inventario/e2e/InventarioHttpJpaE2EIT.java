package com.sigo.inventario.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class InventarioHttpJpaE2EIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("sigo_inventory_e2e")
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
    private Long puestoId;

    @BeforeEach
    void prepararUsuarioReal() {
        plazaId = jdbc.queryForObject(
                """
                insert into plazas(codigo, descripcion, activo)
                values ('P4-E2E', 'Plaza E2E', true)
                returning id
                """,
                Long.class
        );

        puestoId = jdbc.queryForObject(
                """
                insert into puestos(nombre)
                values ('Agente E2E Inventario')
                returning id
                """,
                Long.class
        );

        jdbc.update(
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
                """,
                99301,
                "Operador Inventario E2E",
                puestoId,
                plazaId
        );

        Long rolId = jdbc.queryForObject(
                """
                insert into inventario_rol(codigo, nombre, activo)
                values ('AGENTE_E2E', 'Agente E2E', true)
                returning id
                """,
                Long.class
        );

        jdbc.update(
                """
                insert into inventario_puesto_rol(puesto_id, rol_id)
                values (?, ?)
                """,
                puestoId,
                rolId
        );
    }

    @Test
    void jwtHttpAplicacionJpaYPostgresFuncionanEnUnSoloFlujo()
            throws Exception {

        mockMvc.perform(
                        get("/api/inventario/me")
                                .with(jwtOperador())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(99301))
                .andExpect(
                        jsonPath("$.nombre")
                                .value("Operador Inventario E2E")
                )
                .andExpect(
                        jsonPath("$.rol")
                                .value("AGENTE_E2E")
                )
                .andExpect(
                        jsonPath("$.plaza")
                                .value("P4-E2E")
                );

        mockMvc.perform(
                        post("/api/inventarios")
                                .with(jwtOperador())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(
                        jsonPath("$.plazaId")
                                .value(plazaId)
                )
                .andExpect(
                        jsonPath("$.estado")
                                .value("EN_PROCESO")
                );

        Integer total = jdbc.queryForObject(
                """
                select count(*)
                from inventario_conteo i
                join trabajadores t
                  on t.id = i.responsable_id
                where t.codigo = 99301
                  and i.plaza_id = ?
                  and i.estado = 'EN_PROCESO'
                """,
                Integer.class,
                plazaId
        );

        assertNotNull(total);
        assertEquals(1, total);
    }

    private RequestPostProcessor jwtOperador() {
        return jwt()
                .jwt(token ->
                        token
                                .subject("99301")
                                .claim("rol", "OPERADOR")
                )
                .authorities(
                        new SimpleGrantedAuthority(
                                "ROLE_OPERADOR"
                        )
                );
    }
}
