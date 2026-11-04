package com.sigo.inventario.application.service;

import com.sigo.inventario.application.port.in.CorregirInventarioUseCase;
import com.sigo.inventario.application.port.in.InventarioConsultaUseCase;
import com.sigo.inventario.application.port.out.InventarioAuditoriaPort;
import com.sigo.inventario.application.port.out.InventarioConsultaPort;
import com.sigo.inventario.application.port.out.InventarioCorreccionPort;
import com.sigo.inventario.domain.InventarioEstado;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ConflictException;
import com.sigo.shared.exception.ForbiddenException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class CorregirInventarioServiceTest {
    private final InventarioConsultaPort consulta = mock(InventarioConsultaPort.class);
    private final InventarioCorreccionPort correccion = mock(InventarioCorreccionPort.class);
    private final InventarioAuditoriaPort auditoria = mock(InventarioAuditoriaPort.class);
    private final InventarioConsultaUseCase consultas = mock(InventarioConsultaUseCase.class);

    private final CorregirInventarioService service =
            new CorregirInventarioService(consulta, correccion, auditoria, consultas);

    private final List<CorregirInventarioUseCase.Item> productos =
            List.of(new CorregirInventarioUseCase.Item(100L, new BigDecimal("6.00")));

    @Test
    void supervisorCorrigeYRegistraValoresAnterioresConMotivo() {
        when(consulta.requireInventario(42L)).thenReturn(cabecera(InventarioEstado.FINALIZADO));
        when(correccion.corregir(eq(42L), anyList())).thenReturn(
                List.of(new InventarioCorreccionPort.Cambio(100L,
                        new BigDecimal("4.00"), new BigDecimal("6.00"))));

        service.corregir(usuario("SUPERVISOR", 8L), 42L, productos, "Error al contar");
        verify(correccion).corregir(42L, productos);
        verify(auditoria).registrar(eq(10L), eq("INVENTARIO_CORREGIDO"),
                eq("INVENTARIO_CONTEO"), eq(42L),
                argThat(d -> d.get("motivo").equals("Error al contar")
                        && ((List<?>) d.get("cambios")).size() == 1));
    }

    @Test
    void controladorNoPuedeCorregirOtraPlaza() {
        when(consulta.requireInventario(42L)).thenReturn(cabecera(InventarioEstado.FINALIZADO));
        assertThrows(ForbiddenException.class, () ->
                service.corregir(usuario("CONTROLADOR", 8L), 42L, productos, "Error de conteo"));
        verifyNoInteractions(correccion, auditoria);
    }

    @Test
    void operadorNoPuedeCorregirInclusoEnSuPlaza() {
        when(consulta.requireInventario(42L)).thenReturn(cabecera(InventarioEstado.FINALIZADO));
        assertThrows(ForbiddenException.class, () ->
                service.corregir(usuario("OPERADOR", 4L), 42L, productos, "Error de conteo"));
        verifyNoInteractions(correccion, auditoria);
    }

    @Test
    void rechazaInventarioAnulado() {
        when(consulta.requireInventario(42L)).thenReturn(cabecera(InventarioEstado.ANULADO));
        assertThrows(ConflictException.class, () ->
                service.corregir(usuario("SUPERVISOR", 4L), 42L, productos, "Error de conteo"));
        verifyNoInteractions(correccion, auditoria);
    }

    @Test
    void rechazaDuplicadosYCantidadesNegativas() {
        when(consulta.requireInventario(42L)).thenReturn(cabecera(InventarioEstado.FINALIZADO));
        assertThrows(BusinessException.class, () ->
                service.corregir(usuario("SUPERVISOR", 4L), 42L,
                        List.of(productos.get(0), productos.get(0)), "Error al contar"));
        assertThrows(BusinessException.class, () ->
                service.corregir(usuario("SUPERVISOR", 4L), 42L,
                        List.of(new CorregirInventarioUseCase.Item(100L, new BigDecimal("-1"))),
                        "Error al contar"));
        verifyNoInteractions(correccion, auditoria);
    }

    @Test
    void rechazaGuardarSinCambios() {
        when(consulta.requireInventario(42L)).thenReturn(cabecera(InventarioEstado.FINALIZADO));
        when(correccion.corregir(eq(42L), anyList())).thenReturn(List.of());
        assertThrows(BusinessException.class, () ->
                service.corregir(usuario("CONTROLADOR", 4L), 42L, productos, "Error de conteo"));
        verifyNoInteractions(auditoria);
    }

    private CorregirInventarioUseCase.Usuario usuario(String rol, Long plazaId) {
        return new CorregirInventarioUseCase.Usuario(10L, plazaId, rol, rol);
    }

    private InventarioConsultaPort.Cabecera cabecera(InventarioEstado estado) {
        return new InventarioConsultaPort.Cabecera(
                42L, 4L, "P4", 10L, 287, "Responsable", 2L, "CONTROLADOR",
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now().minusDays(1),
                estado, null, null);
    }
}
