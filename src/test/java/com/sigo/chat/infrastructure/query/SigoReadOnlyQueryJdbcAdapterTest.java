package com.sigo.chat.infrastructure.query;

import com.sigo.chat.domain.SigoTool;
import com.sigo.chat.domain.SigoToolRequest;
import com.sigo.chat.domain.SigoToolResult;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SigoReadOnlyQueryJdbcAdapterTest {
    private SigoToolRequest consulta(SigoTool tool, String trabajador, int limite) {
        return new SigoToolRequest(tool, null, trabajador, "P4", null, null,
                null, null, null,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), limite);
    }

    @Test
    void informaTotalDashboardYDetalleCompletoConLimite() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenAnswer(call -> {
            String query = call.getArgument(0);
            if (query.contains("FROM asistencia_ausencia aa")) {
                assertTrue(query.contains("COUNT(*) OVER() AS total_ausencias_detalladas"));
                Object[] args = call.getArgument(1);
                assertEquals(2, args[args.length - 1]);
                return List.of(
                        Map.<String, Object>of("trabajador", "Ana", "total_ausencias_detalladas", 47L),
                        Map.<String, Object>of("trabajador", "Luis", "total_ausencias_detalladas", 47L));
            }
            assertTrue(query.contains("SUM(ar.programados - ar.presentes)"));
            return List.of(Map.<String, Object>of("ausentes", 54L));
        });

        SigoToolResult result = new SigoReadOnlyQueryJdbcAdapter(jdbc)
                .ejecutar(consulta(SigoTool.AUSENCIAS, null, 2));
        assertEquals(2, result.filas().size());
        assertTrue(result.nota().contains("Dashboard (programados menos presentes): 54"));
        assertTrue(result.nota().contains("Se muestran 2 de 47"));
        assertFalse(result.filas().getFirst().containsKey("total_ausencias_detalladas"));
        verify(jdbc, times(2)).queryForList(anyString(), any(Object[].class));
    }

    @Test
    void resumenGeneralReutilizaLaCifraDelDashboard() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenAnswer(call -> {
            String query = call.getArgument(0);
            assertFalse(query.contains("FROM asistencia_ausencia aa"));
            if (query.contains("FROM asistencia_registro ar")) {
                return List.of(Map.<String, Object>of("ausentes", 31L, "registros", 12L));
            }
            return List.of(Map.<String, Object>of("relevos", 3L));
        });

        SigoToolResult result = new SigoReadOnlyQueryJdbcAdapter(jdbc)
                .ejecutar(consulta(SigoTool.RESUMEN_GENERAL, null, 20));
        assertEquals(3, result.filas().size());
        assertEquals(31L, result.filas().get(1).get("total_ausencias"));
        verify(jdbc, times(2)).queryForList(anyString(), any(Object[].class));
    }

    @Test
    void alFiltrarTrabajadorSoloInformaDetallesCorrespondientes() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(
                List.of(Map.<String, Object>of("trabajador", "Ana", "total_ausencias_detalladas", 3L)));
        SigoToolResult result = new SigoReadOnlyQueryJdbcAdapter(jdbc)
                .ejecutar(consulta(SigoTool.AUSENCIAS, "Ana", 1));
        assertTrue(result.nota().contains("Se muestran 1 de 3"));
        assertFalse(result.nota().contains("Dashboard"));
        verify(jdbc, times(1)).queryForList(anyString(), any(Object[].class));
    }
}
