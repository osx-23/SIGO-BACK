package com.sigo.programacion.infrastructure.service;

import com.sigo.programacion.infrastructure.persistence.entity.DistribucionPersonal;
import com.sigo.programacion.infrastructure.persistence.entity.EstadoProgramacion;
import com.sigo.programacion.infrastructure.persistence.entity.ProgramacionTurno;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    private ProgramacionTurno turno(Long id, LocalDate fecha, EstadoProgramacion estado) {
        var turno = mock(ProgramacionTurno.class);
        when(turno.getId()).thenReturn(id);
        when(turno.getFecha()).thenReturn(fecha);
        when(turno.getEstado()).thenReturn(estado);
        return turno;
    }
}
