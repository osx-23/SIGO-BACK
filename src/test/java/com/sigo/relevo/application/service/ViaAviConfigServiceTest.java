package com.sigo.relevo.application.service;

import com.sigo.relevo.application.port.in.ConfigurarViasAviUseCase;
import com.sigo.relevo.application.port.out.ViaConsultaPort;
import com.sigo.security.application.port.in.UsuarioActualUseCase;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ForbiddenException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ViaAviConfigServiceTest {

    @Test
    void soloSupervisorPuedeListarConfiguracion() {
        ViaConsultaPort port =
                mock(ViaConsultaPort.class);
        UsuarioActualUseCase actual =
                mock(UsuarioActualUseCase.class);

        when(actual.requireActual())
                .thenReturn(usuario("OPERADOR"));

        ViaAviConfigService service =
                new ViaAviConfigService(
                        port,
                        actual
                );

        assertThrows(
                ForbiddenException.class,
                () -> service.listar(4L)
        );

        verifyNoInteractions(port);
    }

    @Test
    void supervisorNoPuedeOcultarLaUltimaViaVisible() {
        ViaConsultaPort port =
                mock(ViaConsultaPort.class);
        UsuarioActualUseCase actual =
                mock(UsuarioActualUseCase.class);

        when(actual.requireActual())
                .thenReturn(usuario("SUPERVISOR"));
        when(port.existePlaza(4L))
                .thenReturn(true);
        when(port.listarActivasParaConfigAvi(4L))
                .thenReturn(
                        List.of(
                                via(1L, 101, true),
                                via(2L, 102, false)
                        )
                );

        ViaAviConfigService service =
                new ViaAviConfigService(
                        port,
                        actual
                );

        assertThrows(
                BusinessException.class,
                () -> service.actualizar(
                        4L,
                        1L,
                        false
                )
        );

        verify(port, never())
                .actualizarVisibilidadAvi(
                        anyLong(),
                        anyLong(),
                        anyBoolean()
                );
    }

    @Test
    void supervisorPuedeOcultarUnaViaSiQuedaOtraVisible() {
        ViaConsultaPort port =
                mock(ViaConsultaPort.class);
        UsuarioActualUseCase actual =
                mock(UsuarioActualUseCase.class);

        when(actual.requireActual())
                .thenReturn(usuario("SUPERVISOR"));
        when(port.existePlaza(4L))
                .thenReturn(true);
        when(port.listarActivasParaConfigAvi(4L))
                .thenReturn(
                        List.of(
                                via(1L, 101, true),
                                via(2L, 102, true)
                        )
                );

        ViaAviConfigService service =
                new ViaAviConfigService(
                        port,
                        actual
                );

        assertDoesNotThrow(
                () -> service.actualizar(
                        4L,
                        1L,
                        false
                )
        );

        verify(port)
                .actualizarVisibilidadAvi(
                        4L,
                        1L,
                        false
                );
    }

    private ConfigurarViasAviUseCase.ViaConfig via(
            Long id,
            Integer numero,
            boolean visible
    ) {
        return new ConfigurarViasAviUseCase.ViaConfig(
                id,
                4L,
                numero,
                "Vía " + numero,
                true,
                numero,
                visible
        );
    }

    private UsuarioActualUseCase.UsuarioActual usuario(
            String rol
    ) {
        return new UsuarioActualUseCase.UsuarioActual(
                10L,
                2396,
                "Usuario AVIX",
                rol,
                4L,
                "P4",
                1L,
                "Agente"
        );
    }
}
