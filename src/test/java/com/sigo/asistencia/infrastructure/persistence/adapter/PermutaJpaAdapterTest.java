package com.sigo.asistencia.infrastructure.persistence.adapter;

import com.sigo.asistencia.application.port.in.PermutaUseCase;
import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Puesto;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.entity.Turno;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.personal.infrastructure.persistence.repository.TurnoRepository;
import com.sigo.shared.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermutaJpaAdapterTest {
    @Mock JdbcTemplate jdbc;
    @Mock TrabajadorRepository trabajadores;
    @Mock TurnoRepository turnos;
    @Mock PlazaRepository plazas;
    @InjectMocks PermutaJpaAdapter servicio;

    private final LocalDate fecha = LocalDate.of(2026, 11, 16);
    private Plaza plaza;

    @BeforeEach
    void setup() {
        plaza = new Plaza();
        plaza.setId(4L);
        plaza.setCodigo("P4");
        plaza.setActivo(true);
    }

    private Turno turno(long id, String codigo) {
        Turno turno = new Turno();
        turno.setId(id);
        turno.setCodigo(codigo);
        return turno;
    }

    private Trabajador agente(long id, int codigo) {
        Trabajador trabajador = new Trabajador();
        trabajador.setId(id);
        trabajador.setCodigo(codigo);
        trabajador.setNombreCompleto("Agente " + codigo);
        trabajador.setActivo(true);
        Puesto puesto = new Puesto();
        puesto.setNombre("Agente de Recaudación");
        trabajador.setPuesto(puesto);
        trabajador.setPlaza(plaza);
        return trabajador;
    }

    private PermutaUseCase.PermutaCommand cambio() {
        return new PermutaUseCase.PermutaCommand(
                100, fecha, 1L, 4L,
                200, fecha, 2L, 4L
        );
    }

    private void prepararPersonasYTurnos() {
        when(trabajadores.findByCodigo(100)).thenReturn(Optional.of(agente(1, 100)));
        when(trabajadores.findByCodigo(200)).thenReturn(Optional.of(agente(2, 200)));
        when(turnos.findById(1L)).thenReturn(Optional.of(turno(1, "A")));
        when(turnos.findById(2L)).thenReturn(Optional.of(turno(2, "B")));
        when(plazas.findById(4L)).thenReturn(Optional.of(plaza));
    }

    @Test
    void identificaAgenteSinConsultarProgramacion() {
        when(trabajadores.findByCodigo(100)).thenReturn(Optional.of(agente(1, 100)));
        var resultado = servicio.agente(100);
        assertEquals(100, resultado.codigo());
        assertEquals("Agente 100", resultado.nombre());
        verifyNoInteractions(jdbc, turnos, plazas);
    }

    @Test
    void permiteRegistrarJornadasIngresadasManualmente() {
        prepararPersonasYTurnos();
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any(), any()))
                .thenReturn(0);
        servicio.registrarDesdeAsistencia(5L, 4L, 1L, fecha, List.of(cambio()));
        verify(jdbc, times(1)).update(contains("INSERT INTO asistencia_permuta"), any(Object[].class));
    }

    @Test
    void rechazaDobleRegistroDelMismoAgenteEnLaFecha() {
        prepararPersonasYTurnos();
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any(), any()))
                .thenReturn(1);
        ConflictException excepcion = assertThrows(ConflictException.class,
                () -> servicio.registrarDesdeAsistencia(
                        5L, 4L, 1L, fecha, List.of(cambio())));
        assertTrue(excepcion.getMessage().contains("Ya existe una permuta"));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void rechazaPermutaQueNoPerteneceAlTurnoActual() {
        prepararPersonasYTurnos();
        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrarDesdeAsistencia(
                        5L, 4L, 3L, fecha, List.of(cambio())));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }
}
