package com.sigo.inventario.application.service;

import com.sigo.inventario.application.port.out.InventarioProductoVisibilidadPort;
import com.sigo.inventario.application.port.out.InventarioStockQueryPort;
import com.sigo.inventario.application.security.InventarioAuthorizationService;
import com.sigo.inventario.application.security.InventarioUsuarioActual;
import com.sigo.shared.exception.ForbiddenException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventarioStockServiceTest {

    private final InventarioStockQueryPort port = mock(InventarioStockQueryPort.class);
    private final InventarioAuthorizationService autorizacion =
            new InventarioAuthorizationService(mock(InventarioProductoVisibilidadPort.class));
    private final InventarioStockService service =
            new InventarioStockService(port, autorizacion);

    @Test
    void controladorPuedeConsultarStockDeOtraPlazaSinModificarSuAsignacion() {
        var usuario = usuario("CONTROLADOR", 4L);
        when(port.buscar(5L, "conos")).thenReturn(List.of());

        assertEquals(List.of(), service.consultar(usuario, 5L, "conos"));
        verify(port).buscar(5L, "conos");
    }

    @Test
    void controladorPuedeConsultarTodasLasPlazasSiSeleccionaTodas() {
        var usuario = usuario("CONTROLADOR", 4L);
        when(port.buscar(null, null)).thenReturn(List.of());

        assertEquals(List.of(), service.consultar(usuario, null, null));
        verify(port).buscar(null, null);
    }

    @Test
    void controladorSinPlazaAsignadaNoPuedeConsultarStock() {
        assertThrows(ForbiddenException.class,
                () -> service.consultar(usuario("CONTROLADOR", null), null, null));
    }

    @Test
    void operadorNoPuedeConsultarOtraPlaza() {
        assertThrows(ForbiddenException.class,
                () -> service.consultar(usuario("OPERADOR", 4L), 5L, null));
    }

    @Test
    void operadorSoloConsultaSuPlazaCuandoNoEspecificaFiltro() {
        var usuario = usuario("OPERADOR", 4L);
        when(port.buscar(4L, null)).thenReturn(List.of());

        assertEquals(List.of(), service.consultar(usuario, null, null));
        verify(port).buscar(4L, null);
    }

    @Test
    void supervisorPuedeConsultarTodasLasPlazas() {
        when(port.buscar(null, null)).thenReturn(List.of());
        assertEquals(List.of(), service.consultar(usuario("SUPERVISOR", null), null, null));
        verify(port).buscar(null, null);
    }

    private InventarioUsuarioActual usuario(String rol, Long plazaId) {
        return new InventarioUsuarioActual(
                10L, 2396, "Usuario de prueba", plazaId,
                plazaId == null ? null : "P4", 2L, rol, rol);
    }
}
