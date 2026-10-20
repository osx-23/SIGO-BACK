package com.sigo.shared.scheduling;

import com.sigo.inventario.infrastructure.persistence.repository.InventarioConteoRepository;
import com.sigo.relevo.application.port.out.RelevoSesionPort;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class SesionTurnoVencimientoJobTest {
    @Test
    void anulaAmbosTiposDeSesionesSiLaTablaExiste() {
        var inventarios = mock(InventarioConteoRepository.class);
        var relevos = mock(RelevoSesionPort.class);
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject("SELECT to_regclass('relevo_sesiones') IS NOT NULL", Boolean.class))
                .thenReturn(true);
        var job = new SesionTurnoVencimientoJob(inventarios, relevos, jdbc);
        job.anularSesionesVencidas();
        verify(inventarios).anularVencidos();
        verify(relevos).anularVencidos();
    }

    @Test
    void noInterrumpeInventariosSiNoExisteMigracionDeRelevo() {
        var inventarios = mock(InventarioConteoRepository.class);
        var relevos = mock(RelevoSesionPort.class);
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject("SELECT to_regclass('relevo_sesiones') IS NOT NULL", Boolean.class))
                .thenReturn(false);
        var job = new SesionTurnoVencimientoJob(inventarios, relevos, jdbc);
        job.anularSesionesVencidas();
        verify(inventarios).anularVencidos();
        verifyNoInteractions(relevos);
    }
}
