package com.sigo.relevo.api.controller;

import com.sigo.relevo.application.port.in.ConsultarRelevosUseCase;
import com.sigo.relevo.application.port.in.GestionarEvidenciaRelevoUseCase;
import com.sigo.relevo.application.port.in.GestionarRelevoUseCase;
import com.sigo.relevo.application.port.in.ListarViasUseCase;
import com.sigo.relevo.application.port.in.RelevoAccesoUseCase;
import com.sigo.relevo.application.port.in.RelevoHistorialUseCase;
import com.sigo.security.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {
                RelevoController.class,
                ViaController.class
        },
        properties = "app.jwt.secret=01234567890123456789012345678901"
)
@Import(SecurityConfig.class)
class RelevoApiAuthorizationIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConsultarRelevosUseCase consultaUseCase;

    @MockBean
    private GestionarRelevoUseCase gestionarUseCase;

    @MockBean
    private GestionarEvidenciaRelevoUseCase evidenciaUseCase;

    @MockBean
    private RelevoHistorialUseCase historialUseCase;

    @MockBean
    private RelevoAccesoUseCase accesoUseCase;

    @MockBean
    private ListarViasUseCase listarViasUseCase;

    @Test
    void anonimoNoPuedeConsultarElementos() throws Exception {
        mockMvc.perform(get("/api/relevos/elementos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void operadorPuedeConsultarElementos() throws Exception {
        when(consultaUseCase.listarElementos())
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/relevos/elementos")
                                .with(jwtRol("OPERADOR"))
                )
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(consultaUseCase).listarElementos();
    }

    @Test
    void operadorPuedeConsultarViasDePlaza() throws Exception {
        when(listarViasUseCase.listarPorPlaza(anyLong()))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/vias")
                                .param("plazaId", "4")
                                .with(jwtRol("OPERADOR"))
                )
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(listarViasUseCase).listarPorPlaza(4L);
    }

    @Test
    void controladorPuedeConsultarElementos() throws Exception {
        when(consultaUseCase.listarElementos())
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/relevos/elementos")
                                .with(jwtRol("CONTROLADOR"))
                )
                .andExpect(status().isOk());
    }

    @Test
    void supervisorPuedeConsultarElementos() throws Exception {
        when(consultaUseCase.listarElementos())
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/relevos/elementos")
                                .with(jwtRol("SUPERVISOR"))
                )
                .andExpect(status().isOk());
    }

    @Test
    void requestRelevoInvalidoDevuelveBadRequest() throws Exception {
        mockMvc.perform(
                        post("/api/relevos")
                                .with(jwtRol("OPERADOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest());
    }

    private RequestPostProcessor jwtRol(String rol) {
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
