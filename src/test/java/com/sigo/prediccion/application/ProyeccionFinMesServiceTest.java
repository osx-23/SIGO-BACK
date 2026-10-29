package com.sigo.prediccion.application;

import com.sigo.prediccion.application.port.out.ProyeccionDatosPort;
import com.sigo.prediccion.domain.PrediccionAsistenciaParametros;
import com.sigo.prediccion.domain.PrediccionAsistenciaResultado;
import com.sigo.prediccion.domain.ProyeccionFinMesResultado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProyeccionFinMesServiceTest {
    private final ProyeccionDatosPort datos = mock(ProyeccionDatosPort.class);
    private final PrediccionAsistenciaService ml = mock(PrediccionAsistenciaService.class);
    private ProyeccionFinMesService service;
    private final LocalDate hoy = LocalDate.of(2026, 10, 28);

    @BeforeEach
    void preparar() {
        service = new ProyeccionFinMesService(datos, ml);
        when(datos.listarTurnos()).thenReturn(List.of(new ProyeccionDatosPort.TurnoOperativo(2L, "B")));
        when(datos.asistencias(4L, LocalDate.of(2026, 10, 29), LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of());
    }

    @Test
    void calculaSoloFechasConProgramacionReal() {
        var fecha = LocalDate.of(2026, 10, 29);
        var filas = List.of(
                new ProyeccionDatosPort.TurnoProgramado(fecha, 100L, "B"),
                new ProyeccionDatosPort.TurnoProgramado(fecha, 101L, "B"),
                new ProyeccionDatosPort.TurnoProgramado(fecha, 100L, "B"));
        when(datos.programacion(4L, fecha, LocalDate.of(2026, 10, 31))).thenReturn(filas);
        when(ml.predecir(any(PrediccionAsistenciaParametros.class)))
                .thenReturn(new PrediccionAsistenciaResultado(4L, 2L, fecha,
                        2, 1, 1, "random_forest", "0.1.0", "real", null));

        ProyeccionFinMesResultado resultado = service.proyectar(4L, 2L, hoy);
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
        when(datos.programacion(4L, desde, LocalDate.of(2026, 10, 31))).thenReturn(List.of());
        ProyeccionFinMesResultado resultado = service.proyectar(4L, 2L, hoy);
        assertEquals(0, resultado.turnosProyectados());
        assertEquals(3, resultado.turnosSinProgramacion());
        verifyNoInteractions(ml);
    }

    @Test
    void noPrediceTurnosConAsistenciaYaRegistrada() {
        var fecha = LocalDate.of(2026, 10, 29);
        when(datos.programacion(4L, fecha, LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(new ProyeccionDatosPort.TurnoProgramado(fecha, 100L, "B")));
        when(datos.asistencias(4L, fecha, LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(new ProyeccionDatosPort.AsistenciaRegistrada(fecha, 2L)));
        ProyeccionFinMesResultado resultado = service.proyectar(4L, 2L, hoy);
        assertEquals("YA_REGISTRADO", resultado.detalle().get(0).estado());
        assertNull(resultado.detalle().get(0).ausenciasEstimadas());
        assertEquals(1, resultado.turnosYaRegistrados());
        verifyNoInteractions(ml);
    }

    @Test
    void ultimoDiaDelMesDevuelveProyeccionVacia() {
        var resultado = service.proyectar(4L, 2L, LocalDate.of(2026, 10, 31));
        assertTrue(resultado.detalle().isEmpty());
        verifyNoInteractions(ml);
    }
}
