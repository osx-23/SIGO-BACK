package com.sigo.relevo.api.controller;

import com.sigo.relevo.application.port.in.ListarViasUseCase;
import com.sigo.security.application.port.in.UsuarioActualUseCase;
import com.sigo.shared.exception.ForbiddenException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ViaAviControllerTest {

    @Test
    void usuarioAviConsultaSoloSuPlaza() {
        ListarViasUseCase vias =
                mock(ListarViasUseCase.class);
        UsuarioActualUseCase actual =
                mock(UsuarioActualUseCase.class);

        when(actual.requireActual())
                .thenReturn(usuario(4L, "P4"));
        when(vias.listarVisiblesAvi(4L))
                .thenReturn(List.of());

        ViaAviController controller =
                new ViaAviController(
                        vias,
                        actual
                );

        assertDoesNotThrow(
                () -> controller.listarVisibles(4L)
        );

        verify(vias).listarVisiblesAvi(4L);
    }

    @Test
    void usuarioAviNoConsultaOtraPlaza() {
        ListarViasUseCase vias =
                mock(ListarViasUseCase.class);
        UsuarioActualUseCase actual =
                mock(UsuarioActualUseCase.class);

        when(actual.requireActual())
                .thenReturn(usuario(4L, "P4"));

        ViaAviController controller =
                new ViaAviController(
                        vias,
                        actual
                );

        assertThrows(
                ForbiddenException.class,
                () -> controller.listarVisibles(5L)
        );

        verify(vias, never())
                .listarVisiblesAvi(anyLong());
    }

    private UsuarioActualUseCase.UsuarioActual usuario(
            Long plazaId,
            String plazaCodigo
    ) {
        return new UsuarioActualUseCase.UsuarioActual(
                10L,
                2396,
                "Usuario AVIX",
                "OPERADOR",
                plazaId,
                plazaCodigo,
                1L,
                "Agente"
        );
    }
}
