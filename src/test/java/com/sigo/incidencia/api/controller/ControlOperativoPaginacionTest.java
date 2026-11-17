package com.sigo.incidencia.api.controller;

import com.sigo.incidencia.application.port.out.IncidenciaStoragePort;
import com.sigo.security.application.port.in.UsuarioActualUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ControlOperativoPaginacionTest {
    @Mock JdbcTemplate jdbc;
    @Mock UsuarioActualUseCase usuarioActual;
    @Mock IncidenciaStoragePort storage;
    @InjectMocks ControlOperativoController controller;

    @Test
    void paginaPosteriorAlLimiteHistoricoDe500EsConsultable() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 12, 31);
        when(jdbc.queryForObject(contains("count(*)"), eq(Long.class),
                any(Object[].class))).thenReturn(501L);
        when(jdbc.queryForList(contains("limit ? offset ?"),
                any(Object[].class))).thenReturn(List.of(Map.of("id", 501L)));

        var resultado = controller.listarPaginado(null, null, desde, hasta, 50, 10);

        assertEquals(501, resultado.total());
        assertEquals(51, resultado.totalPaginas());
        assertEquals(50, resultado.pagina());
        assertEquals(1, resultado.registros().size());
        verify(jdbc).queryForList(contains("limit ? offset ?"),
                argThat((Object[] args) -> args.length == 4 &&
                        args[2].equals(10) && args[3].equals(500L)));
    }

    @Test
    void paginaFueraDeRangoNoConsultaRegistros() {
        when(jdbc.queryForObject(contains("count(*)"), eq(Long.class),
                any(Object[].class))).thenReturn(12L);

        var resultado = controller.listarPaginado(null, null,
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), 2, 10);

        assertEquals(12, resultado.total());
        assertEquals(2, resultado.totalPaginas());
        assertTrue(resultado.registros().isEmpty());
        verify(jdbc, never()).queryForList(anyString(), any(Object[].class));
    }

    @Test
    void rechazaTamanoExcesivo() {
        assertThrows(ResponseStatusException.class,
                () -> controller.listarPaginado(null, null,
                        LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 11, 30), 0, 101));
        verifyNoInteractions(jdbc);
    }
}
