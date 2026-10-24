package com.sigo.programacion.infrastructure.report;

import com.sigo.personal.application.port.in.TrabajadorUseCase;
import com.sigo.programacion.application.port.in.GuardarTurnosUseCase;
import com.sigo.programacion.application.port.in.DistribucionUseCase;
import com.sigo.programacion.domain.ProgramacionValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DistribucionExcelGuardarServiceTest {
    @Test
    void bloqueaGuardadoDeTurnosCuandoBanderaEsFalse() {
        var previo = mock(DistribucionExcelTurnosPreviewService.class);
        var personal = mock(TrabajadorUseCase.class);
        var guardar = mock(GuardarTurnosUseCase.class);
        var servicio = new DistribucionExcelTurnosGuardarService(previo, personal, guardar);
        assertThrows(ProgramacionValidationException.class,
                () -> servicio.confirmar(3L, new byte[]{1}, 2026, 9));
        verifyNoInteractions(previo, personal, guardar);
    }

    @Test
    void bloqueaGuardadoDeCasetasCuandoBanderaEsFalse() {
        var previo = mock(DistribucionExcelImportService.class);
        var guardar = mock(DistribucionUseCase.class);
        var personal = mock(TrabajadorUseCase.class);
        var servicio = new DistribucionExcelCasetasGuardarService(previo, guardar, personal);
        assertThrows(ProgramacionValidationException.class,
                () -> servicio.confirmar(3L, new byte[]{1}, 2026, 9));
        verifyNoInteractions(previo, guardar, personal);
    }

    @Test
    void noEscribeTurnosSiLaVistaYaCoincide() {
        var previo = mock(DistribucionExcelTurnosPreviewService.class);
        var personal = mock(TrabajadorUseCase.class);
        var guardar = mock(GuardarTurnosUseCase.class);
        var servicio = new DistribucionExcelTurnosGuardarService(previo, personal, guardar);
        habilitar(servicio);
        var fecha = java.time.LocalDate.of(2026, 9, 1);
        var propuesta = new DistribucionExcelTurnosPreviewService.Propuesta(2396, "DEMO", fecha,
                "104", "A", "A", "104", "COINCIDE", "");
        when(previo.previsualizar(eq(3L), any(byte[].class), eq(2026), eq(9))).thenReturn(
                new DistribucionExcelTurnosPreviewService.VistaPrevia(fecha, fecha, 1, 0, 0, 1, 0,
                        java.util.List.of(propuesta), java.util.List.of()));
        when(personal.listarAdministracion(3L)).thenReturn(java.util.List.of());
        var resultado = servicio.confirmar(3L, new byte[]{1}, 2026, 9);
        assertEquals(0, resultado.guardados());
        verifyNoInteractions(guardar);
    }

    @Test
    void turnoNuevoSeGuardaSoloSiEstaActivoYEsOperativo() {
        var previo = mock(DistribucionExcelTurnosPreviewService.class);
        var personal = mock(TrabajadorUseCase.class);
        var guardar = mock(GuardarTurnosUseCase.class);
        var servicio = new DistribucionExcelTurnosGuardarService(previo, personal, guardar);
        habilitar(servicio);
        var fecha = java.time.LocalDate.of(2026, 9, 1);
        var propuesta = new DistribucionExcelTurnosPreviewService.Propuesta(2396, "DEMO", fecha,
                "104", "A", null, null, "CREAR", "");
        when(previo.previsualizar(eq(3L), any(byte[].class), eq(2026), eq(9))).thenReturn(
                new DistribucionExcelTurnosPreviewService.VistaPrevia(fecha, fecha, 1, 1, 0, 0, 0,
                        java.util.List.of(propuesta), java.util.List.of()));
        when(personal.listarAdministracion(3L)).thenReturn(java.util.List.of(
                new TrabajadorUseCase.TrabajadorData(1L, 2396, "DEMO", null, null, null, false, true)));
        var resultado = servicio.confirmar(3L, new byte[]{1}, 2026, 9);
        assertEquals(1, resultado.guardados());
        verify(guardar).guardar(argThat(cmd -> cmd.programaciones().size() == 1 &&
                "A".equals(cmd.programaciones().get(0).estado())));
    }

    @Test
    void casetaDeTrabajadorInactivoSeOmiteSinCambiarTurnos() {
        var previo = mock(DistribucionExcelImportService.class);
        var guardar = mock(DistribucionUseCase.class);
        var personal = mock(TrabajadorUseCase.class);
        var servicio = new DistribucionExcelCasetasGuardarService(previo, guardar, personal);
        habilitar(servicio);
        var fecha = java.time.LocalDate.of(2026, 9, 1);
        var cambio = new DistribucionExcelImportService.Cambio(21L, 104L, 2396, "DEMO", fecha, "104");
        when(previo.previsualizar(eq(3L), any(byte[].class), eq(2026), eq(9))).thenReturn(
                new DistribucionExcelImportService.VistaPrevia(true, 3L, fecha, fecha, 1, 0,
                        java.util.List.of(cambio), java.util.List.of(), java.util.List.of()));
        when(personal.listarAdministracion(3L)).thenReturn(java.util.List.of(
                new TrabajadorUseCase.TrabajadorData(1L, 2396, "DEMO", null, null, null, false, false)));
        var resultado = servicio.confirmar(3L, new byte[]{1}, 2026, 9);
        assertEquals(0, resultado.guardados());
        assertEquals(1, resultado.omitidos());
        verifyNoInteractions(guardar);
    }

    @Test
    void casetaValidaUsaSoloDistribucionYNoGuardarTurnos() {
        var previo = mock(DistribucionExcelImportService.class);
        var guardar = mock(DistribucionUseCase.class);
        var personal = mock(TrabajadorUseCase.class);
        var servicio = new DistribucionExcelCasetasGuardarService(previo, guardar, personal);
        habilitar(servicio);
        var fecha = java.time.LocalDate.of(2026, 9, 1);
        var cambio = new DistribucionExcelImportService.Cambio(21L, 104L, 2396, "DEMO", fecha, "104");
        when(previo.previsualizar(eq(3L), any(byte[].class), eq(2026), eq(9))).thenReturn(
                new DistribucionExcelImportService.VistaPrevia(true, 3L, fecha, fecha, 1, 0,
                        java.util.List.of(cambio), java.util.List.of(), java.util.List.of("otro sin turno")));
        when(personal.listarAdministracion(3L)).thenReturn(java.util.List.of(
                new TrabajadorUseCase.TrabajadorData(1L, 2396, "DEMO", null, null, null, false, true)));
        var resultado = servicio.confirmar(3L, new byte[]{1}, 2026, 9);
        assertEquals(1, resultado.guardados());
        assertEquals(1, resultado.omitidos());
        verify(guardar).guardar(argThat(cmd -> cmd.distribuciones().size() == 1 &&
                cmd.distribuciones().get(0).programacionTurnoId().equals(21L)), eq(true));
    }

    @Test
    void excelPuedeSobrescribirTurnoDeDescansoMedicoPreviamenteEditado() {
        var previo = mock(DistribucionExcelTurnosPreviewService.class);
        var personal = mock(TrabajadorUseCase.class);
        var guardar = mock(GuardarTurnosUseCase.class);
        var servicio = new DistribucionExcelTurnosGuardarService(previo, personal, guardar);
        habilitar(servicio);
        var fecha = java.time.LocalDate.of(2026, 9, 1);
        var p = new DistribucionExcelTurnosPreviewService.Propuesta(2396, "DEMO", fecha,
                "104", "A", "DM", null, "ACTUALIZAR", "Excel confirmado");
        when(previo.previsualizar(eq(3L), any(byte[].class), eq(2026), eq(9))).thenReturn(
                new DistribucionExcelTurnosPreviewService.VistaPrevia(fecha, fecha, 1, 0, 1, 0, 0,
                        java.util.List.of(p), java.util.List.of()));
        when(personal.listarAdministracion(3L)).thenReturn(java.util.List.of(
                new TrabajadorUseCase.TrabajadorData(1L, 2396, "DEMO", null, null, null, false, true)));
        var res = servicio.confirmar(3L, new byte[] {1}, 2026, 9);
        assertEquals(1, res.guardados());
        verify(guardar).guardar(argThat(c -> "A".equals(c.programaciones().get(0).estado())));
    }

    private static void habilitar(Object servicio) {
        try {
            var campo = servicio.getClass().getDeclaredField("habilitado");
            campo.setAccessible(true);
            campo.setBoolean(servicio, true);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
