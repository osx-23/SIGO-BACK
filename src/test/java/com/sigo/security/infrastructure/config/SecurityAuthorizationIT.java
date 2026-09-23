package com.sigo.security.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({
        SecurityConfig.class,
        SecurityAuthorizationIT.ProbeController.class
})
class SecurityAuthorizationIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonimoNoPuedeAccederARutasProtegidas() throws Exception {
        mockMvc.perform(get("/api/relevos/probe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void operadorPuedeRelevosInventarioOperativoYMiHorario() throws Exception {
        mockMvc.perform(
                        get("/api/relevos/probe")
                                .with(jwtRol("OPERADOR"))
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/inventarios/probe")
                                .with(jwtRol("OPERADOR"))
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/programacion/mi-horario")
                                .with(jwtRol("OPERADOR"))
                )
                .andExpect(status().isOk());
    }

    @Test
    void operadorNoPuedeAsistenciaDashboardNiAdministracionInventario()
            throws Exception {
        mockMvc.perform(
                        get("/api/asistencias/probe")
                                .with(jwtRol("OPERADOR"))
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get("/api/dashboard/probe")
                                .with(jwtRol("OPERADOR"))
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get("/api/inventario/productos/probe")
                                .with(jwtRol("OPERADOR"))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void controladorPuedeAsistenciaDashboardYAdministracionInventario()
            throws Exception {
        mockMvc.perform(
                        get("/api/asistencias/probe")
                                .with(jwtRol("CONTROLADOR"))
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/dashboard/probe")
                                .with(jwtRol("CONTROLADOR"))
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/inventario/productos/probe")
                                .with(jwtRol("CONTROLADOR"))
                )
                .andExpect(status().isOk());
    }

    @Test
    void controladorNoPuedeGenerarProgramacionNiModificarSecuencias()
            throws Exception {
        mockMvc.perform(
                        post("/api/programacion/generar-propuesta")
                                .with(jwtRol("CONTROLADOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        put("/api/programacion/secuencias/1")
                                .with(jwtRol("CONTROLADOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void supervisorPuedeRutasDeGestionExclusiva() throws Exception {
        mockMvc.perform(
                        post("/api/programacion/generar-propuesta")
                                .with(jwtRol("SUPERVISOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        put("/api/programacion/secuencias/1")
                                .with(jwtRol("SUPERVISOR"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/programacion/grupos/probe")
                                .with(jwtRol("SUPERVISOR"))
                )
                .andExpect(status().isOk());
    }

    @Test
    void cualquierUsuarioAutenticadoPuedeRutaNoEspecial() throws Exception {
        mockMvc.perform(
                        get("/api/other/probe")
                                .with(jwtRol("OPERADOR"))
                )
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor
    jwtRol(String rol) {
        return jwt()
                .jwt(jwt -> jwt
                        .subject("999")
                        .claim("rol", rol)
                )
                .authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_" + rol
                        )
                );
    }

    @RestController
    static class ProbeController {

        @GetMapping("/api/relevos/probe")
        String relevos() {
            return "ok";
        }

        @GetMapping("/api/inventarios/probe")
        String inventarios() {
            return "ok";
        }

        @GetMapping("/api/programacion/mi-horario")
        String miHorario() {
            return "ok";
        }

        @GetMapping("/api/asistencias/probe")
        String asistencias() {
            return "ok";
        }

        @GetMapping("/api/dashboard/probe")
        String dashboard() {
            return "ok";
        }

        @GetMapping("/api/inventario/productos/probe")
        String productos() {
            return "ok";
        }

        @PostMapping("/api/programacion/generar-propuesta")
        String generar() {
            return "ok";
        }

        @PutMapping("/api/programacion/secuencias/{id}")
        String secuencia(@PathVariable Long id) {
            return "ok";
        }

        @GetMapping("/api/programacion/grupos/probe")
        String grupos() {
            return "ok";
        }

        @GetMapping("/api/other/probe")
        String other() {
            return "ok";
        }
    }
}
