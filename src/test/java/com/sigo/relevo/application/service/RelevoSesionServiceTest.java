package com.sigo.relevo.application.service;

import com.sigo.relevo.application.port.in.ConsultarRelevosUseCase;
import com.sigo.relevo.application.port.in.GestionarRelevoUseCase;
import com.sigo.relevo.application.port.in.RelevoAccesoUseCase;
import com.sigo.relevo.application.port.in.RelevoHistorialUseCase;
import com.sigo.relevo.application.port.in.RelevoSesionUseCase;
import com.sigo.relevo.application.port.out.RelevoSesionPort;
import com.sigo.relevo.application.port.out.RelevoUsuarioActualPort;
import com.sigo.shared.exception.ForbiddenException;
import com.sigo.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelevoSesionServiceTest {
    @Mock RelevoSesionPort sesiones;
    @Mock RelevoUsuarioActualPort usuarios;
    @Mock GestionarRelevoUseCase gestionar;
    @Mock RelevoAccesoUseCase acceso;
    @Mock RelevoHistorialUseCase historialUseCase;
    @InjectMocks RelevoSesionService servicio;

    private final RelevoSesionUseCase.Sesion abierta = new RelevoSesionUseCase.Sesion(
            17L, 100L, 4L, 1L, "EN_PROCESO",
            OffsetDateTime.now().minusMinutes(30),
            null, null, null);

    @BeforeEach
    void usuarioActual() {
        when(usuarios.requireActual()).thenReturn(
                new RelevoUsuarioActualPort.UsuarioActual(100L, "OPERADOR", 4L));
    }

    @Test
    void iniciarRecuperaUnaSesionPreexistente() {
        when(sesiones.enProceso(100L)).thenReturn(Optional.of(abierta));
        var actual = servicio.iniciar(4L, 1L);
        assertEquals(17L, actual.id());
        verify(sesiones, never()).crear(any(), any(), any());
    }

    @Test
    void noPermiteIniciarRelevoEnOtraPlaza() {
        assertThrows(ForbiddenException.class, () -> servicio.iniciar(3L, 1L));
        verifyNoInteractions(sesiones);
    }

    @Test
    void otraPersonaNoPuedeFinalizarSesion() {
        when(sesiones.obtener(17L)).thenReturn(Optional.of(new RelevoSesionUseCase.Sesion(
                17L, 999L, 4L, 1L, "EN_PROCESO", abierta.iniciadoEn(), null, null, null)));
        var solicitud = solicitud();
        assertThrows(ForbiddenException.class, () -> servicio.finalizar(17L, solicitud));
        verifyNoInteractions(gestionar);
    }

    @Test
    void finalizarRegistraRelevoYMarcaSesionComoFinalizada() {
        when(sesiones.obtener(17L)).thenReturn(Optional.of(abierta));
        var solicitud = solicitud();
        when(acceso.prepararRegistro(solicitud)).thenReturn(solicitud);
        var relevo = mock(ConsultarRelevosUseCase.Relevo.class);
        when(relevo.id()).thenReturn(55L);
        when(gestionar.registrar(solicitud)).thenReturn(relevo);
        var terminada = new RelevoSesionUseCase.Sesion(
                17L, 100L, 4L, 1L, "FINALIZADO", abierta.iniciadoEn(),
                OffsetDateTime.parse("2026-10-20T06:25:00-05:00"), 55L, null);
        when(sesiones.finalizar(17L, 55L)).thenReturn(terminada);

        var result = servicio.finalizar(17L, solicitud);

        assertEquals(55L, result.relevoId());
        assertEquals("FINALIZADO", result.estado());
        verify(sesiones).finalizar(17L, 55L);
    }

    @Test
    void noPermiteFinalizarUnaSesionVencidaHaceOchoHoras() {
        var vencida = new RelevoSesionUseCase.Sesion(
                17L, 100L, 4L, 1L, "EN_PROCESO",
                OffsetDateTime.now().minusHours(8).minusMinutes(1),
                null, null, null);
        when(sesiones.obtener(17L)).thenReturn(Optional.of(vencida));

        assertThrows(BusinessException.class, () -> servicio.finalizar(17L, solicitud()));
        verifyNoInteractions(gestionar);
    }

    private GestionarRelevoUseCase.Command solicitud() {
        return new GestionarRelevoUseCase.Command(
                4L, 1L, 100L, LocalDate.parse("2026-10-20"), LocalTime.of(6, 0),
                null, null, List.of(), List.of());
    }
}
