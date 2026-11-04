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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CorregirInventarioService implements CorregirInventarioUseCase {
    private final InventarioConsultaPort consultaPort;
    private final InventarioCorreccionPort correccionPort;
    private final InventarioAuditoriaPort auditoria;
    private final InventarioConsultaUseCase consultaUseCase;

    @Override
    @Transactional
    public InventarioConsultaUseCase.Detalle corregir(
            Usuario usuario, Long inventarioId, List<Item> productos, String motivo) {
        if (usuario == null || usuario.trabajadorId() == null) {
            throw new ForbiddenException("Usuario inválido");
        }

        var inventario = consultaPort.requireInventario(inventarioId);
        boolean supervisor = "SUPERVISOR".equalsIgnoreCase(usuario.rolSistema());
        boolean controladorDePlaza = "CONTROLADOR".equalsIgnoreCase(usuario.rolSistema())
                && usuario.plazaId() != null
                && usuario.plazaId().equals(inventario.plazaId());
        if (!supervisor && !controladorDePlaza) {
            throw new ForbiddenException("Solo supervisores o controladores de la plaza pueden corregir inventarios");
        }
        if (inventario.estado() != InventarioEstado.FINALIZADO) {
            throw new ConflictException("Solo se pueden corregir inventarios FINALIZADOS");
        }

        String motivoLimpio = motivo == null ? "" : motivo.trim();
        if (motivoLimpio.length() < 5 || motivoLimpio.length() > 500) {
            throw new BusinessException("Indique un motivo de corrección de 5 a 500 caracteres");
        }
        if (productos == null || productos.isEmpty()) {
            throw new BusinessException("El inventario debe contener productos");
        }
        Set<Long> ids = new HashSet<>();
        for (Item item : productos) {
            if (item == null || item.productoId() == null || !ids.add(item.productoId())) {
                throw new BusinessException("Hay productos inválidos o repetidos");
            }
            BigDecimal cantidad = item.cantidad();
            if (cantidad == null || cantidad.signum() < 0
                    || cantidad.scale() > 2 || cantidad.precision() - cantidad.scale() > 12) {
                throw new BusinessException("Las cantidades deben estar entre 0 y 999999999999.99, con máximo 2 decimales");
            }
        }

        List<InventarioCorreccionPort.Cambio> cambios = correccionPort.corregir(inventarioId, productos);
        if (cambios.isEmpty()) {
            throw new BusinessException("No hay cantidades modificadas");
        }

        auditoria.registrar(usuario.trabajadorId(), "INVENTARIO_CORREGIDO", "INVENTARIO_CONTEO",
                inventarioId, Map.of(
                        "motivo", motivoLimpio,
                        "cambios", cambios.stream().map(c -> Map.of(
                                "productoId", c.productoId(),
                                "anterior", c.anterior().toPlainString(),
                                "nuevo", c.nuevo().toPlainString()
                        )).toList(),
                        "numeroCambios", cambios.size()
                ));

        return consultaUseCase.detalle(
                new InventarioConsultaUseCase.Usuario(
                        usuario.trabajadorId(), usuario.plazaId(), usuario.rolInventario()),
                inventarioId);
    }
}
