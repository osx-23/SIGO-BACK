package com.sigo.relevo.api.controller;

import com.sigo.relevo.application.port.in.ConsultarRelevosUseCase;
import com.sigo.relevo.application.port.in.GestionarEvidenciaRelevoUseCase;
import com.sigo.relevo.application.port.in.GestionarRelevoUseCase;
import com.sigo.relevo.application.port.in.ListarViasUseCase;
import com.sigo.relevo.application.port.in.RelevoAccesoUseCase;
import com.sigo.relevo.application.port.in.RelevoHistorialUseCase;
import com.sigo.security.infrastructure.config.SecurityConfig;
import com.sigo.shared.exception.ConflictException;
import com.sigo.shared.exception.ResourceNotFoundException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

    @Test
    void relevoInexistenteDevuelveNotFound() throws Exception {
        when(historialUseCase.obtenerPara(
                isNull(),
                eq(999L)
        )).thenThrow(
                new ResourceNotFoundException(
                        "Relevo no encontrado"
                )
        );

        mockMvc.perform(
                        get("/api/relevos/999")
                                .with(jwtRol("CONTROLADOR"))
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.message")
                                .value("Relevo no encontrado")
                );
    }

    @Test
    void conflictoAlRegistrarDevuelveConflict() throws Exception {
        when(gestionarUseCase.registrar(isNull()))
                .thenThrow(
                        new ConflictException(
                                "Ya existe un relevo para el turno"
                        )
                );

        mockMvc.perform(
                        post("/api/relevos")
                                .with(jwtRol("OPERADOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "plazaId": 4,
                                          "turnoId": 1,
                                          "operadorId": 101,
                                          "fecha": "2026-09-23",
                                          "hora": "08:00:00",
                                          "checklist": [
                                            {
                                              "elementoId": 1,
                                              "estado": "OPERATIVO",
                                              "cantidad": 1
                                            }
                                          ],
                                          "vias": []
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Ya existe un relevo para el turno"
                                )
                );
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
