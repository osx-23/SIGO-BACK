package com.sigo.programacion.infrastructure.service;

import com.sigo.programacion.application.port.in.GeneradorAsignacionCasetasUseCase.TipoPeriodo;
import com.sigo.programacion.domain.ProgramacionValidationException;
import com.sigo.programacion.infrastructure.persistence.entity.DistribucionPersonal;
import com.sigo.programacion.infrastructure.persistence.entity.EstadoProgramacion;
import com.sigo.programacion.infrastructure.persistence.entity.ProgramacionTurno;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeneradorAsignacionCasetasJpaServiceTest {

    @Test
    void soloGeneraParaTurnosSinCaseta() {
        var yaAsignado = turno(1L, LocalDate.of(2026, 11, 1), EstadoProgramacion.A);
        var pendiente = turno(2L, LocalDate.of(2026, 11, 1), EstadoProgramacion.A);
        var distribucion = mock(DistribucionPersonal.class);
        when(distribucion.getProgramacionTurno()).thenReturn(yaAsignado);

        var resultado = GeneradorAsignacionCasetasJpaService.turnosSinCaseta(
                List.of(yaAsignado, pendiente), List.of(distribucion));

        assertEquals(List.of(pendiente), resultado);
    }

    @Test
    void noGeneraCuandoTodosYaTienenCaseta() {
        var turno = turno(1L, LocalDate.of(2026, 11, 1), EstadoProgramacion.B);
        var distribucion = mock(DistribucionPersonal.class);
        when(distribucion.getProgramacionTurno()).thenReturn(turno);

        assertTrue(GeneradorAsignacionCasetasJpaService.turnosSinCaseta(
                List.of(turno), List.of(distribucion)).isEmpty());
    }

    @Test
    void bloqueaLasCasetasOcupadasSoloEnElMismoDiaYTurno() {
        var fecha = LocalDate.of(2026, 11, 1);
        var turnoPendiente = turno(3L, fecha, EstadoProgramacion.A);

        Set<Long> reservadas = GeneradorAsignacionCasetasJpaService.casetasOcupadasEnGrupo(
                List.of(turnoPendiente),
                Map.of(
                        "11|2026-11-01|A", 101L,
                        "12|2026-11-01|A", 102L,
                        "13|2026-11-01|B", 103L,
                        "14|2026-11-02|A", 104L,
                        "15|2026-10-31|A", 105L));

        assertEquals(Set.of(101L, 102L), reservadas);
    }

    @Test
    void generaSoloDesdeHoyHastaFechaSeleccionada() {
        var rango = GeneradorAsignacionCasetasJpaService.resolverRangoGeneracion(
                YearMonth.of(2026, 11), TipoPeriodo.MES, null,
                LocalDate.of(2026, 11, 18), LocalDate.of(2026, 11, 23));

        assertEquals(LocalDate.of(2026, 11, 18), rango.desde());
        assertEquals(LocalDate.of(2026, 11, 23), rango.hasta());
    }

    @Test
    void permiteHastaFinDeMesYConservaModoMensualOriginal() {
        var mes = YearMonth.of(2026, 11);
        var total = GeneradorAsignacionCasetasJpaService.resolverRangoGeneracion(
                mes, TipoPeriodo.MES, null, null, null);
        var hastaFin = GeneradorAsignacionCasetasJpaService.resolverRangoGeneracion(
                mes, TipoPeriodo.MES, null, LocalDate.of(2026, 11, 18), mes.atEndOfMonth());

        assertEquals(mes.atDay(1), total.desde());
        assertEquals(mes.atEndOfMonth(), total.hasta());
        assertEquals(LocalDate.of(2026, 11, 18), hastaFin.desde());
        assertEquals(mes.atEndOfMonth(), hastaFin.hasta());
    }

    @Test
    void noAceptaRangosFueraDelMesNiInvertidos() {
        var mes = YearMonth.of(2026, 11);

        assertThrows(ProgramacionValidationException.class, () ->
                GeneradorAsignacionCasetasJpaService.resolverRangoGeneracion(
                        mes, TipoPeriodo.MES, null,
                        LocalDate.of(2026, 11, 24), LocalDate.of(2026, 11, 18)));
        assertThrows(ProgramacionValidationException.class, () ->
                GeneradorAsignacionCasetasJpaService.resolverRangoGeneracion(
                        mes, TipoPeriodo.MES, null,
                        LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 30)));
        assertThrows(ProgramacionValidationException.class, () ->
                GeneradorAsignacionCasetasJpaService.resolverRangoGeneracion(
                        mes, TipoPeriodo.SEMANA, 3,
                        LocalDate.of(2026, 11, 18), LocalDate.of(2026, 11, 23)));
    }

    private ProgramacionTurno turno(Long id, LocalDate fecha, EstadoProgramacion estado) {
        var turno = mock(ProgramacionTurno.class);
        when(turno.getId()).thenReturn(id);
        when(turno.getFecha()).thenReturn(fecha);
        when(turno.getEstado()).thenReturn(estado);
        return turno;
    }
}
