package com.sigo.asistencia.api.controller;

import com.sigo.asistencia.application.port.in.AsistenciaAccesoUseCase;
import com.sigo.asistencia.application.port.in.ConsultarAsistenciasUseCase;
import com.sigo.asistencia.application.port.in.GestionarAsistenciaUseCase;
import com.sigo.asistencia.application.port.in.GestionarEvidenciaAsistenciaUseCase;
import com.sigo.asistencia.application.port.in.ObtenerProgramadosAsistenciaUseCase;
import com.sigo.asistencia.application.port.in.RegistrarAsistenciaExcepcionUseCase;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AsistenciaController.class,
        properties = "app.jwt.secret=01234567890123456789012345678901"
)
@Import(SecurityConfig.class)
class AsistenciaApiAuthorizationIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GestionarAsistenciaUseCase gestionarUseCase;

    @MockBean
    private GestionarEvidenciaAsistenciaUseCase evidenciaUseCase;

    @MockBean
    private ConsultarAsistenciasUseCase consultaUseCase;

    @MockBean
    private RegistrarAsistenciaExcepcionUseCase asistenciaExcepcionUseCase;

    @MockBean
    private ObtenerProgramadosAsistenciaUseCase programacionService;

    @MockBean
    private AsistenciaAccesoUseCase accesoUseCase;

    @Test
    void anonimoNoPuedeConsultarAsistencias() throws Exception {
        mockMvc.perform(get("/api/asistencias"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void operadorNoPuedeConsultarAsistencias() throws Exception {
        mockMvc.perform(
                        get("/api/asistencias")
                                .with(jwtRol("OPERADOR"))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void controladorPuedeConsultarAsistencias() throws Exception {
        when(consultaUseCase.listar(
                any(),
                any(),
                any()
        )).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/asistencias")
                                .with(jwtRol("CONTROLADOR"))
                )
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(consultaUseCase).listar(
                null,
                null,
                null
        );
    }

    @Test
    void supervisorPuedeConsultarAsistencias() throws Exception {
        when(consultaUseCase.listar(
                any(),
                any(),
                any()
        )).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/asistencias")
                                .with(jwtRol("SUPERVISOR"))
                )
                .andExpect(status().isOk());
    }

    @Test
    void requestInvalidoDevuelveBadRequestAntesDeRegistrar() throws Exception {
        mockMvc.perform(
                        post("/api/asistencias")
                                .with(jwtRol("CONTROLADOR"))
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
