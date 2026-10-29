package com.sigo.prediccion.application;

import com.sigo.asistencia.infrastructure.persistence.entity.AsistenciaRegistro;
import com.sigo.asistencia.infrastructure.persistence.repository.AsistenciaRegistroRepository;
import com.sigo.personal.infrastructure.persistence.entity.Turno;
import com.sigo.personal.infrastructure.persistence.repository.TurnoRepository;
import com.sigo.prediccion.api.dto.ProyeccionFinMesResponse;
import com.sigo.prediccion.infrastructure.ProyeccionFinMesService;
import com.sigo.prediccion.domain.PrediccionAsistenciaParametros;
import com.sigo.prediccion.domain.PrediccionAsistenciaResultado;
import com.sigo.programacion.infrastructure.persistence.entity.EstadoProgramacion;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionTurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProyeccionFinMesServiceTest {
    private final ProgramacionTurnoRepository programacion = mock(ProgramacionTurnoRepository.class);
    private final AsistenciaRegistroRepository asistencia = mock(AsistenciaRegistroRepository.class);
    private final TurnoRepository turnos = mock(TurnoRepository.class);
    private final PrediccionAsistenciaService ml = mock(PrediccionAsistenciaService.class);
    private ProyeccionFinMesService service;

    private final LocalDate hoy = LocalDate.of(2026, 10, 28);

    @BeforeEach
    void preparar() {
        service = new ProyeccionFinMesService(programacion, asistencia, turnos, ml);
        Turno b = new Turno();
        b.setId(2L);
        b.setCodigo("B");
        when(turnos.findAll()).thenReturn(List.of(b));
        when(asistencia.findByFechaBetweenAndPlazaIdOrderByFechaDescIdDesc(
                LocalDate.of(2026, 10, 29), LocalDate.of(2026, 10, 31), 4L))
                .thenReturn(List.of());
    }

    private ProgramacionTurnoRepository.TurnoResumen turno(LocalDate fecha, long trabajadorId) {
        var fila = mock(ProgramacionTurnoRepository.TurnoResumen.class);
        when(fila.getFecha()).thenReturn(fecha);
        when(fila.getTrabajadorId()).thenReturn(trabajadorId);
        when(fila.getEstado()).thenReturn(EstadoProgramacion.B);
        return fila;
    }

    @Test
    void calculaSoloFechasConProgramacionReal() {
        var fecha = LocalDate.of(2026, 10, 29);
        when(programacion.findMesResumen(4L, fecha, LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(turno(fecha, 100L), turno(fecha, 101L), turno(fecha, 100L)));
        when(ml.predecir(any(PrediccionAsistenciaParametros.class)))
                .thenReturn(new PrediccionAsistenciaResultado(4L, 2L, fecha,
                        2, 1, 1, "random_forest", "0.1.0", "real", null));

        ProyeccionFinMesResponse resultado = service.proyectar(4L, 2L, hoy);

        assertEquals(1, resultado.turnosProyectados());
        assertEquals(2, resultado.turnosSinProgramacion());
        assertEquals(2, resultado.totalProgramados());
        assertEquals(1, resultado.totalAusenciasEstimadas());
        assertEquals(3, resultado.detalle().size());
        assertEquals("SIN_PROGRAMACION", resultado.detalle().get(1).estado());
        verify(ml, times(1)).predecir(new PrediccionAsistenciaParametros(4L, 2L, fecha, 2));
    }

    @Test
    void noInventaPrediccionesCuandoNoHayProgramacion() {
        var desde = LocalDate.of(2026, 10, 29);
        when(programacion.findMesResumen(4L, desde, LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of());

        ProyeccionFinMesResponse resultado = service.proyectar(4L, 2L, hoy);

        assertEquals(0, resultado.turnosProyectados());
        assertEquals(3, resultado.turnosSinProgramacion());
        verifyNoInteractions(ml);
    }

    @Test
    void noPrediceTurnosConAsistenciaYaRegistrada() {
        var fecha = LocalDate.of(2026, 10, 29);
        when(programacion.findMesResumen(4L, fecha, LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(turno(fecha, 100L)));
        var registro = new AsistenciaRegistro();
        registro.setFecha(fecha);
        Turno b = new Turno();
        b.setId(2L);
        registro.setTurno(b);
        when(asistencia.findByFechaBetweenAndPlazaIdOrderByFechaDescIdDesc(
                fecha, LocalDate.of(2026, 10, 31), 4L))
                .thenReturn(List.of(registro));

        ProyeccionFinMesResponse resultado = service.proyectar(4L, 2L, hoy);
        assertEquals("YA_REGISTRADO", resultado.detalle().get(0).estado());
        assertNull(resultado.detalle().get(0).ausenciasEstimadas());
        assertEquals(1, resultado.turnosYaRegistrados());
        verifyNoInteractions(ml);
    }

    @Test
    void ultimoDiaDelMesDevuelveProyeccionVacia() {
        var resultado = service.proyectar(4L, 2L, LocalDate.of(2026, 10, 31));
        assertTrue(resultado.detalle().isEmpty());
        verifyNoInteractions(programacion, asistencia, ml);
    }
}
