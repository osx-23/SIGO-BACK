package com.sigo.inventario.application.service;

import com.sigo.inventario.application.port.in.GuardarConteoInventarioUseCase;
import com.sigo.inventario.application.port.in.InventarioConsultaUseCase;
import com.sigo.inventario.application.port.out.InventarioConsultaPort;
import com.sigo.inventario.application.port.out.InventarioDetalleGestionPort;
import com.sigo.inventario.application.port.out.InventarioProductoVisibilidadPort;
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
public class GuardarConteoInventarioService
        implements GuardarConteoInventarioUseCase {

    private final InventarioConsultaPort consultaPort;
    private final InventarioDetalleGestionPort detalleGestionPort;
    private final InventarioProductoVisibilidadPort visibilidadPort;
    private final InventarioConsultaUseCase consultaUseCase;
    private final InventarioAuditoriaService auditoria;

    @Override
    @Transactional
    public InventarioConsultaUseCase.Detalle guardar(
            Usuario usuario,
            Long inventarioId,
            List<Item> productos
    ) {
        if (usuario == null
                || usuario.trabajadorId() == null
                || usuario.plazaId() == null
                || usuario.rolId() == null) {
            throw new ForbiddenException("Usuario de inventario inválido"
            );
        }

        if (productos == null || productos.isEmpty()) {
            throw new BusinessException("Debe registrar al menos un producto"
            );
        }

        InventarioConsultaPort.Cabecera inventario =
                consultaPort.requireInventario(inventarioId);

        if (!inventario.responsableId().equals(
                usuario.trabajadorId()
        )) {
            throw new ForbiddenException("El inventario pertenece a otro trabajador"
            );
        }

        if (inventario.estado()
                != InventarioEstado.EN_PROCESO) {
            throw new ConflictException("Solo se puede editar un inventario EN_PROCESO"
            );
        }

        Set<Long> ids = new HashSet<>();

        for (Item item : productos) {
            if (item == null || item.productoId() == null) {
                throw new BusinessException("Producto inválido"
                );
            }

            if (!ids.add(item.productoId())) {
                throw new BusinessException("Producto repetido"
                );
            }

            if (item.cantidad() == null) {
                throw new BusinessException("La cantidad es obligatoria"
                );
            }

            if (item.cantidad().compareTo(
                    BigDecimal.ZERO
            ) < 0) {
                throw new BusinessException("La cantidad no puede ser negativa"
                );
            }

            boolean visible = visibilidadPort.esVisiblePara(
                    item.productoId(),
                    usuario.rolId(),
                    usuario.plazaId()
            );

            if (!visible) {
                throw new ForbiddenException("Producto no autorizado para el rol o plaza"
                );
            }
        }

        detalleGestionPort.guardar(
                inventarioId,
                productos
        );

        auditoria.registrar(
                usuario.trabajadorId(),
                "CONTEO_GUARDADO",
                "INVENTARIO_CONTEO",
                inventarioId,
                Map.of(
                        "productosRecibidos",
                        productos.size()
                )
        );

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
