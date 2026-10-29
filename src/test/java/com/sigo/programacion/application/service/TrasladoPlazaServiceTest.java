package com.sigo.programacion.application.service;

import com.sigo.programacion.application.port.out.ProgramacionAccessPort;
import com.sigo.programacion.application.port.out.TrasladoPlazaPort;
import com.sigo.programacion.domain.ProgramacionValidationException;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.ZoneId;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TrasladoPlazaServiceTest {
    private final TrasladoPlazaPort port = mock(TrasladoPlazaPort.class);
    private final ProgramacionAccessPort access = mock(ProgramacionAccessPort.class);
    private final TrasladoPlazaService service = new TrasladoPlazaService(port, access);

    @Test
    void validaFechaInicioAntesDeRegistrar() {
        LocalDate ayer = LocalDate.now(ZoneId.of("America/Lima")).minusDays(1);
        assertThrows(ProgramacionValidationException.class,
                () -> service.crear(8L, 2L, ayer, null));
        verifyNoInteractions(port);
    }

    @Test
    void temporalNoPuedeTerminarAntesDelInicio() {
        LocalDate inicio = LocalDate.now(ZoneId.of("America/Lima")).plusDays(4);
        assertThrows(ProgramacionValidationException.class,
                () -> service.crear(8L, 2L, inicio, inicio.minusDays(1)));
        verifyNoInteractions(port);
    }

    @Test
    void admiteTrasladoDefinitivoFuturo() {
        LocalDate inicio = LocalDate.now(ZoneId.of("America/Lima")).plusDays(10);
        when(access.requireSupervisorId()).thenReturn(12L);
        service.crear(8L, 2L, inicio, null);
        verify(port).crear(8L, 2L, inicio, null, 12L);
    }

    @Test
    void cancelarRequiereSupervisorYEnviaId() {
        service.cancelar(15L);
        verify(access).requireSupervisorId();
        verify(port).cancelar(15L);
    }
}
