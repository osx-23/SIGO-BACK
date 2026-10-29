package com.sigo.programacion.infrastructure.persistence.adapter;

import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.programacion.infrastructure.persistence.repository.DistribucionPersonalRepository;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionTurnoRepository;
import com.sigo.programacion.infrastructure.persistence.repository.TrasladoPlazaRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class TrasladoPlazaJpaAdapterConsultaTest {

    private final TrasladoPlazaRepository repo = mock(TrasladoPlazaRepository.class);
    private final TrasladoPlazaJpaAdapter adapter = new TrasladoPlazaJpaAdapter(
        repo, mock(TrabajadorRepository.class), mock(PlazaRepository.class),
        mock(ProgramacionTurnoRepository.class), mock(DistribucionPersonalRepository.class)
    );

    @Test
    void consultaPeriodoSinParametrosNulos() {
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 31);
        when(repo.consultarPlazaPeriodo(3L, desde, hasta)).thenReturn(List.of());

        assertTrue(adapter.listar(3L, desde, hasta).isEmpty());

        verify(repo).consultarPlazaPeriodo(3L, desde, hasta);
        verify(repo, never()).consultarTodos();
        verify(repo, never()).consultarPlaza(anyLong());
    }

    @Test
    void consultaSoloPlazaCuandoNoHayPeriodo() {
        when(repo.consultarPlaza(3L)).thenReturn(List.of());

        assertTrue(adapter.listar(3L, null, null).isEmpty());

        verify(repo).consultarPlaza(3L);
        verify(repo, never()).consultarTodos();
        verify(repo, never()).consultarPlazaPeriodo(anyLong(), any(), any());
    }

    @Test
    void consultaTodosSinFiltros() {
        when(repo.consultarTodos()).thenReturn(List.of());

        assertTrue(adapter.listar(null, null, null).isEmpty());

        verify(repo).consultarTodos();
        verify(repo, never()).consultarPlaza(anyLong());
        verify(repo, never()).consultarPlazaPeriodo(anyLong(), any(), any());
    }
}
