package com.sigo.asistencia.infrastructure.persistence.adapter;

import com.sigo.asistencia.application.port.in.PermutaUseCase.PermutaDetalle;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.personal.infrastructure.persistence.repository.TurnoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermutaResumenAnualTest {
    @Mock JdbcTemplate jdbc;
    @Mock TrabajadorRepository trabajadores;
    @Mock TurnoRepository turnos;
    @Mock PlazaRepository plazas;
    @InjectMocks PermutaJpaAdapter adapter;

    private PermutaDetalle registro(long id, String plazaSolicitante, String plazaAceptante,
                                     LocalDate fechaSolicitante, LocalDate fechaAceptante) {
        return new PermutaDetalle(
                id, plazaSolicitante.equals("P1") ? 1L : 2L, plazaSolicitante,
                2000 + (int) id, "Solicitante " + id, 1L, "A", fechaSolicitante,
                plazaAceptante.equals("P1") ? 1L : 2L, plazaAceptante,
                3000 + (int) id, "Aceptante " + id, 2L, "B", fechaAceptante);
    }

    @Test
    void presentaDoceMesesYCuentaCadaIntercambioUnaSolaVezPorPlazaOriginal() {
        var registros = List.of(
                registro(1, "P1", "P2", LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 5)),
                registro(2, "P2", "P1", LocalDate.of(2026, 1, 7), LocalDate.of(2026, 1, 7)),
                registro(3, "P1", "P1", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1)),
                registro(4, "P1", "P2", LocalDate.of(2025, 12, 31), LocalDate.of(2026, 1, 1))
        );
        doReturn(registros).when(jdbc).query(anyString(),
                org.mockito.ArgumentMatchers.<RowMapper<PermutaDetalle>>any(),
                any(Date.class), any(Date.class), any(Date.class), any(Date.class), isNull());
        when(jdbc.queryForList(anyString(), eq(String.class))).thenReturn(List.of("P1", "P2"));

        var anual = adapter.resumenAnual(2026);

        assertEquals(12, anual.meses().size());
        assertEquals(3, anual.total());
        assertEquals(2, anual.meses().get(0).total());
        assertEquals(1, anual.meses().get(1).total());
        assertEquals(0, anual.meses().get(11).total());
        assertEquals(2, anual.totalesPorPlaza().get(0).total());
        assertEquals(1, anual.totalesPorPlaza().get(1).total());
        assertEquals(6, anual.topAgentes().size());
    }

    @Test
    void anioFueraDeRangoNoAccedeABaseDatos() {
        assertThrows(IllegalArgumentException.class, () -> adapter.resumenAnual(1999));
        verifyNoInteractions(jdbc);
    }
}
