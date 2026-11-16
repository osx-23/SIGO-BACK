package com.sigo.asistencia.infrastructure.persistence.adapter;

import com.sigo.asistencia.application.port.in.PermutaUseCase;
import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Puesto;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.entity.Turno;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.personal.infrastructure.persistence.repository.TurnoRepository;
import com.sigo.programacion.infrastructure.persistence.entity.EstadoProgramacion;
import com.sigo.programacion.infrastructure.persistence.entity.ProgramacionTurno;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionTurnoRepository;
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
    @Mock ProgramacionTurnoRepository programaciones;
    @Mock TurnoRepository turnos;
    @InjectMocks PermutaJpaAdapter servicio;

    private final LocalDate fecha = LocalDate.of(2026, 11, 16);
    private Plaza plaza;

    @BeforeEach
    void setup() {
        plaza = new Plaza();
        plaza.setId(4L);
        plaza.setCodigo("P4");
    }

    private Turno turno(long id, String codigo) {
        Turno turno = new Turno();
        turno.setId(id);
        turno.setCodigo(codigo);
        return turno;
    }

    private Trabajador agente(long id, int codigo, boolean activo) {
        Trabajador trabajador = new Trabajador();
        trabajador.setId(id);
        trabajador.setCodigo(codigo);
        trabajador.setNombreCompleto("Agente " + codigo);
        trabajador.setActivo(activo);
        Puesto puesto = new Puesto();
        puesto.setNombre("Agente de Recaudación");
        trabajador.setPuesto(puesto);
        trabajador.setPlaza(plaza);
        return trabajador;
    }

    private ProgramacionTurno programacion(Trabajador agente, EstadoProgramacion turno) {
        ProgramacionTurno programacion = new ProgramacionTurno();
        programacion.setTrabajador(agente);
        programacion.setPlaza(plaza);
        programacion.setEstado(turno);
        programacion.setFecha(fecha);
        return programacion;
    }

    @Test
    void candidatosSoloMuestraAgentesEnTurnoYNoPermutados() {
        when(turnos.findById(2L)).thenReturn(Optional.of(turno(2, "B")));
        when(jdbc.queryForList(anyString(), eq(Integer.class), any(), any()))
                .thenReturn(List.of(100));
        when(programaciones.findMes(4L, fecha, fecha)).thenReturn(List.of(
                programacion(agente(1, 100, true), EstadoProgramacion.B),
                programacion(agente(2, 200, true), EstadoProgramacion.B),
                programacion(agente(3, 300, true), EstadoProgramacion.A),
                programacion(agente(4, 400, false), EstadoProgramacion.B)
        ));

        var candidatos = servicio.candidatos(4L, 2L, fecha);

        assertEquals(1, candidatos.size());
        assertEquals(200, candidatos.get(0).codigo());
        assertEquals("B", candidatos.get(0).turno());
        assertEquals("P4", candidatos.get(0).plaza());
    }

    @Test
    void prohibeRepetirPermutaSiUnAgenteYaCambioTurnoEseDia() {
        Trabajador solicitante = agente(1, 100, true);
        Trabajador aceptante = agente(2, 200, true);
        when(trabajadores.findByCodigo(100)).thenReturn(Optional.of(solicitante));
        when(trabajadores.findByCodigo(200)).thenReturn(Optional.of(aceptante));
        when(programaciones.findByTrabajadorIdAndFecha(1L, fecha))
                .thenReturn(Optional.of(programacion(solicitante, EstadoProgramacion.A)));
        when(programaciones.findByTrabajadorIdAndFecha(2L, fecha))
                .thenReturn(Optional.of(programacion(aceptante, EstadoProgramacion.B)));
        when(turnos.findAll()).thenReturn(List.of(turno(1, "A"), turno(2, "B")));
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any(), any()))
                .thenReturn(1);

        var permuta = new PermutaUseCase.PermutaCommand(
                100, fecha, 1L, 200, fecha, 2L);

        ConflictException excepcion = assertThrows(ConflictException.class,
                () -> servicio.registrarDesdeAsistencia(
                        5L, 4L, 1L, fecha, List.of(permuta)));
        assertTrue(excepcion.getMessage().contains("Ya existe una permuta"));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }
}
