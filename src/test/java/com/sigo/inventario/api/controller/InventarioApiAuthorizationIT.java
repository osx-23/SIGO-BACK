package com.sigo.inventario.api.controller;

import com.sigo.inventario.application.port.in.CerrarInventarioUseCase;
import com.sigo.inventario.application.port.in.GuardarConteoInventarioUseCase;
import com.sigo.inventario.application.port.in.IniciarInventarioUseCase;
import com.sigo.inventario.application.port.in.InventarioConsultaUseCase;
import com.sigo.inventario.application.port.in.InventarioStockUseCase;
import com.sigo.inventario.application.port.in.ObtenerInventarioUsuarioUseCase;
import com.sigo.inventario.application.security.InventarioUsuarioActual;
import com.sigo.inventario.domain.InventarioEstado;
import com.sigo.security.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {
                InventarioController.class,
                InventarioSesionController.class,
                InventarioStockController.class
        },
        properties = "app.jwt.secret=01234567890123456789012345678901"
)
@Import(SecurityConfig.class)
class InventarioApiAuthorizationIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CerrarInventarioUseCase cerrarUseCase;

    @MockBean
    private GuardarConteoInventarioUseCase guardarConteoUseCase;

    @MockBean
    private IniciarInventarioUseCase iniciarUseCase;

    @MockBean
    private InventarioConsultaUseCase consultaUseCase;

    @MockBean
    private InventarioStockUseCase stockUseCase;

    @MockBean
    private ObtenerInventarioUsuarioUseCase usuarios;

    private InventarioUsuarioActual operador;
    private InventarioUsuarioActual controlador;
    private InventarioUsuarioActual supervisor;

    @BeforeEach
    void prepararUsuarios() {
        operador = new InventarioUsuarioActual(
                101L,
                9301,
                "Operador Test",
                4L,
                "P4",
                1L,
                "AGENTE",
                "OPERADOR"
        );

        controlador = new InventarioUsuarioActual(
                201L,
                550,
                "Controlador Test",
                4L,
                "P4",
                2L,
                "CONTROLADOR",
                "CONTROLADOR"
        );

        supervisor = new InventarioUsuarioActual(
                301L,
                100,
                "Supervisor Test",
                null,
                null,
                3L,
                "SUPERVISOR",
                "SUPERVISOR"
        );
    }

    @Test
    void anonimoNoPuedeConsultarSesionInventario() throws Exception {
        mockMvc.perform(get("/api/inventario/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void operadorPuedeConsultarSuSesion() throws Exception {
        when(usuarios.obtenerActual())
                .thenReturn(operador);

        mockMvc.perform(
                        get("/api/inventario/me")
                                .with(jwtRol("OPERADOR"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trabajadorId").value(101))
                .andExpect(jsonPath("$.codigo").value(9301))
                .andExpect(jsonPath("$.rol").value("AGENTE"))
                .andExpect(jsonPath("$.plazaCodigo").value("P4"));
    }

    @Test
    void operadorPuedeIniciarInventarioOperativo() throws Exception {
        when(usuarios.obtenerActual())
                .thenReturn(operador);

        when(iniciarUseCase.iniciar(any()))
                .thenReturn(
                        new IniciarInventarioUseCase.Resumen(
                                900L,
                                4L,
                                "P4",
                                101L,
                                9301,
                                "Operador Test",
                                "AGENTE",
                                OffsetDateTime.parse(
                                        "2026-09-23T10:00:00-05:00"
                                ),
                                null,
                                InventarioEstado.EN_PROCESO,
                                0
                        )
                );

        mockMvc.perform(
                        post("/api/inventarios")
                                .with(jwtRol("OPERADOR"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(900))
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"))
                .andExpect(jsonPath("$.responsableId").value(101));

        verify(iniciarUseCase).iniciar(
                new IniciarInventarioUseCase.Usuario(
                        101L,
                        4L,
                        1L,
                        "AGENTE"
                )
        );
    }

    @Test
    void operadorNoPuedeConsultarStockDeGestion() throws Exception {
        when(usuarios.obtenerActual())
                .thenReturn(operador);

        mockMvc.perform(
                        get("/api/inventario/stock")
                                .with(jwtRol("OPERADOR"))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void controladorPuedeConsultarStockDeGestion() throws Exception {
        when(usuarios.obtenerActual())
                .thenReturn(controlador);

        when(stockUseCase.consultar(
                any(InventarioUsuarioActual.class),
                isNull(),
                isNull()
        )).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/inventario/stock")
                                .with(jwtRol("CONTROLADOR"))
                )
                .andExpect(status().isOk());

        verify(stockUseCase).consultar(
                controlador,
                null,
                null
        );
    }

    @Test
    void supervisorPuedeConsultarStockDeCualquierPlaza() throws Exception {
        when(usuarios.obtenerActual())
                .thenReturn(supervisor);

        when(stockUseCase.consultar(
                any(InventarioUsuarioActual.class),
                eq(4L),
                isNull()
        )).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/inventario/stock")
                                .param("plazaId", "4")
                                .with(jwtRol("SUPERVISOR"))
                )
                .andExpect(status().isOk());

        verify(stockUseCase).consultar(
                supervisor,
                4L,
                null
        );
    }

    @Test
    void operadorNoPuedeConsultarHistorialGeneral() throws Exception {
        when(usuarios.obtenerActual())
                .thenReturn(operador);

        mockMvc.perform(
                        get("/api/inventarios")
                                .with(jwtRol("OPERADOR"))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void operadorPuedeRecuperarSoloSuInventarioEnProceso() throws Exception {
        when(usuarios.obtenerActual())
                .thenReturn(operador);

        when(consultaUseCase.historial(
                any(InventarioConsultaUseCase.Usuario.class),
                isNull(),
                eq(101L),
                isNull(),
                eq(InventarioEstado.EN_PROCESO),
                isNull(),
                isNull(),
                eq(0),
                eq(1)
        )).thenReturn(
                new InventarioConsultaUseCase.Pagina<>(
                        List.of(),
                        0,
                        1,
                        0,
                        0
                )
        );

        mockMvc.perform(
                        get("/api/inventarios")
                                .param("responsableId", "101")
                                .param("estado", "EN_PROCESO")
                                .param("page", "0")
                                .param("size", "1")
                                .with(jwtRol("OPERADOR"))
                )
                .andExpect(status().isOk());

        verify(consultaUseCase).historial(
                new InventarioConsultaUseCase.Usuario(
                        101L,
                        4L,
                        "AGENTE"
                ),
                null,
                101L,
                null,
                InventarioEstado.EN_PROCESO,
                null,
                null,
                0,
                1
        );
    }

    private RequestPostProcessor jwtRol(
            String rol
    ) {
        return jwt()
                .jwt(token ->
                        token
                                .subject("999")
                                .claim("rol", rol)
                )
                .authorities(
                        new SimpleGrantedAuthority(
                                "ROLE_" + rol
                        )
                );
    }
}
