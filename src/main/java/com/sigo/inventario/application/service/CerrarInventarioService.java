package com.sigo.inventario.application.service;

import com.sigo.inventario.application.port.in.CerrarInventarioUseCase;
import com.sigo.inventario.application.port.in.InventarioConsultaUseCase;
import com.sigo.inventario.application.port.out.InventarioCierrePort;
import com.sigo.inventario.application.port.out.InventarioAuditoriaPort;
import com.sigo.inventario.application.port.out.InventarioConsultaPort;
import com.sigo.inventario.domain.InventarioEstado;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ConflictException;
import com.sigo.shared.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CerrarInventarioService
        implements CerrarInventarioUseCase {

    private final InventarioConsultaPort consultaPort;
    private final InventarioCierrePort cierrePort;
    private final InventarioConsultaUseCase consultaUseCase;
    private final InventarioAuditoriaPort auditoria;

    @Override
    @Transactional
    public InventarioConsultaUseCase.Detalle finalizar(
            Usuario usuario,
            Long inventarioId
    ) {
        InventarioConsultaPort.Cabecera inventario =
                consultaPort.requireInventario(inventarioId);

        if (!inventario.responsableId().equals(
                usuario.trabajadorId()
        )) {
            throw new ForbiddenException(
                    "El inventario pertenece a otro trabajador"
            );
        }

        if (inventario.estado()
                != InventarioEstado.EN_PROCESO) {
            throw new ConflictException(
                    "Inventario ya cerrado"
            );
        }

        var permitidos =
                consultaPort.productosPermitidos(
                        inventario.rolId(),
                        inventario.plazaId()
                );

        var detalles =
                consultaPort.detalleItems(inventarioId);

        Set<Long> productosContados =
                detalles
                        .stream()
                        .map(InventarioConsultaUseCase.DetalleItem::productoId)
                        .collect(Collectors.toSet());

        List<String> faltantes =
                permitidos
                        .stream()
                        .filter(producto ->
                                !productosContados.contains(
                                        producto.id()
                                )
                        )
                        .map(InventarioConsultaUseCase.Producto::nombre)
                        .toList();

        if (!faltantes.isEmpty()) {
            throw new BusinessException(
                    "Faltan productos por contar: "
                            + String.join(", ", faltantes)
            );
        }

        boolean cantidadInvalida =
                detalles
                        .stream()
                        .anyMatch(detalle ->
                                detalle.cantidad() == null
                                        || detalle.cantidad()
                                                .compareTo(
                                                        BigDecimal.ZERO
                                                ) < 0
                        );

        if (cantidadInvalida) {
            throw new BusinessException(
                    "Existe una cantidad inválida"
            );
        }

        cierrePort.finalizar(inventarioId);

        auditoria.registrar(
                usuario.trabajadorId(),
                "INVENTARIO_FINALIZADO",
                "INVENTARIO_CONTEO",
                inventarioId,
                Map.of(
                        "productos",
                        detalles.size()
                )
        );

        return detalleActual(
                usuario,
                inventarioId
        );
    }

    @Override
    @Transactional
    public InventarioConsultaUseCase.Detalle anular(
            Usuario usuario,
            Long inventarioId,
            String motivo
    ) {
        InventarioConsultaPort.Cabecera inventario =
                consultaPort.requireInventario(inventarioId);

        boolean propietarioEnProceso =
                inventario.responsableId().equals(
                        usuario.trabajadorId()
                )
                        && inventario.estado()
                        == InventarioEstado.EN_PROCESO;

        if (!usuario.esSupervisor()
                && !propietarioEnProceso) {
            throw new ForbiddenException(
                    "No puede anular este inventario"
            );
        }

        if (inventario.estado()
                == InventarioEstado.ANULADO) {
            throw new ConflictException(
                    "El inventario ya está anulado"
            );
        }

        if (motivo == null
                || motivo.trim().isBlank()) {
            throw new BusinessException(
                    "El motivo de anulación es obligatorio"
            );
        }

        String motivoLimpio = motivo.trim();

        cierrePort.anular(
                inventarioId,
                usuario.trabajadorId(),
                motivoLimpio
        );

        auditoria.registrar(
                usuario.trabajadorId(),
                "INVENTARIO_ANULADO",
                "INVENTARIO_CONTEO",
                inventarioId,
                Map.of(
                        "motivo",
                        motivoLimpio
                )
        );

        return detalleActual(
                usuario,
                inventarioId
        );
    }

    private InventarioConsultaUseCase.Detalle detalleActual(
            Usuario usuario,
            Long inventarioId
    ) {
        return consultaUseCase.detalle(
                new InventarioConsultaUseCase.Usuario(
                        usuario.trabajadorId(),
                        usuario.plazaId(),
                        usuario.rolCodigo()
                ),
                inventarioId
        );
    }
}
