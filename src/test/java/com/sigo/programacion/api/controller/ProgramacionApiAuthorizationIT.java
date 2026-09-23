package com.sigo.programacion.api.controller;

import com.sigo.programacion.application.port.in.AsignarLiderUseCase;
import com.sigo.programacion.application.port.in.AsignarSecuenciaUseCase;
import com.sigo.programacion.application.port.in.GenerarProgramacionUseCase;
import com.sigo.programacion.application.port.in.GuardarOrdenSecuenciaUseCase;
import com.sigo.programacion.application.port.in.GuardarTurnosUseCase;
import com.sigo.programacion.application.port.in.ListarLideresUseCase;
import com.sigo.programacion.application.port.in.ListarSecuenciasUseCase;
import com.sigo.programacion.application.port.in.ListarTurnosUseCase;
import com.sigo.programacion.application.port.in.MiHorarioUseCase;
import com.sigo.programacion.application.port.in.AgenteProgramacionExcepcionUseCase;
import com.sigo.personal.application.port.in.TrabajadorUseCase;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
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
                ProgramacionController.class,
                ProgramacionGeneradorController.class
        },
        properties = "app.jwt.secret=01234567890123456789012345678901"
)
@Import({
        SecurityConfig.class,
        ProgramacionExceptionHandler.class
})
class ProgramacionApiAuthorizationIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GuardarOrdenSecuenciaUseCase guardarOrdenSecuenciaUseCase;

    @MockBean
    private ListarSecuenciasUseCase listarSecuenciasUseCase;

    @MockBean
    private AsignarSecuenciaUseCase asignarSecuenciaUseCase;

    @MockBean
    private ListarLideresUseCase listarLideresUseCase;

    @MockBean
    private AsignarLiderUseCase asignarLiderUseCase;

    @MockBean
    private ListarTurnosUseCase listarTurnosUseCase;

    @MockBean
    private GuardarTurnosUseCase guardarTurnosUseCase;

    @MockBean
    private MiHorarioUseCase miHorarioUseCase;

    @MockBean
    private GenerarProgramacionUseCase generarProgramacionUseCase;

    @MockBean
    private TrabajadorUseCase trabajadorUseCase;

    @MockBean
    private AgenteProgramacionExcepcionUseCase excepcionUseCase;

    @Test
    void anonimoNoPuedeConsultarMiHorario() throws Exception {
        mockMvc.perform(
                        get("/api/programacion/mi-horario")
                                .param("desde", "2026-09-01")
                                .param("hasta", "2026-09-30")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void operadorPuedeConsultarMiHorario() throws Exception {
        when(miHorarioUseCase.obtener(
                LocalDate.parse("2026-09-01"),
                LocalDate.parse("2026-09-30")
        )).thenReturn(
                new MiHorarioUseCase.Horario(
                        101L,
                        9301,
                        "Operador Test",
                        4L,
                        "P4",
                        "Controlador Test",
                        List.of()
                )
        );

        mockMvc.perform(
                        get("/api/programacion/mi-horario")
                                .param("desde", "2026-09-01")
                                .param("hasta", "2026-09-30")
                                .with(jwtRol("OPERADOR"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trabajadorId").value(101))
                .andExpect(jsonPath("$.plazaCodigo").value("P4"))
                .andExpect(jsonPath("$.dias").isArray());

        verify(miHorarioUseCase).obtener(
                LocalDate.parse("2026-09-01"),
                LocalDate.parse("2026-09-30")
        );
    }

    @Test
    void operadorNoPuedeConsultarTurnosDeGestion() throws Exception {
        mockMvc.perform(
                        get("/api/programacion/turnos")
                                .param("plazaId", "4")
                                .param("anio", "2026")
                                .param("mes", "9")
                                .with(jwtRol("OPERADOR"))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void controladorPuedeConsultarTurnos() throws Exception {
        when(listarTurnosUseCase.listar(
                anyLong(),
                anyInt(),
                anyInt()
        )).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/programacion/turnos")
                                .param("plazaId", "4")
                                .param("anio", "2026")
                                .param("mes", "9")
                                .with(jwtRol("CONTROLADOR"))
                )
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(listarTurnosUseCase).listar(
                4L,
                2026,
                9
        );
    }

    @Test
    void controladorNoPuedeConsultarGrupos() throws Exception {
        mockMvc.perform(
                        get("/api/programacion/grupos")
                                .param("plazaId", "4")
                                .with(jwtRol("CONTROLADOR"))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void supervisorPuedeConsultarGrupos() throws Exception {
        when(listarLideresUseCase.listar(anyLong()))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/programacion/grupos")
                                .param("plazaId", "4")
                                .with(jwtRol("SUPERVISOR"))
                )
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(listarLideresUseCase).listar(4L);
    }

    @Test
    void controladorNoPuedeGenerarPropuesta() throws Exception {
        mockMvc.perform(
                        post("/api/programacion/generar-propuesta")
                                .with(jwtRol("CONTROLADOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void supervisorConRequestInvalidoRecibeBadRequest() throws Exception {
        mockMvc.perform(
                        post("/api/programacion/generar-propuesta")
                                .with(jwtRol("SUPERVISOR"))
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
