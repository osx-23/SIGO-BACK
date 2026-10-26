package com.sigo.inventario.application.service;

import com.sigo.inventario.application.port.in.InventarioStockUseCase;
import com.sigo.inventario.application.port.out.InventarioStockQueryPort;
import com.sigo.inventario.application.security.InventarioAuthorizationService;
import com.sigo.inventario.application.security.InventarioUsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventarioStockService
        implements InventarioStockUseCase {

    private final InventarioStockQueryPort queryPort;
    private final InventarioAuthorizationService auth;

    @Override
    public List<Stock> consultar(
            InventarioUsuarioActual usuario,
            Long plazaId,
            String buscar
    ) {
        Long filtro = plazaId;

        // La consulta de stock es de solo lectura: supervisores y
        // controladores pueden comparar existencias entre plazas.
        // Las operaciones de escritura mantienen sus propias restricciones.
        boolean supervisor = "SUPERVISOR".equalsIgnoreCase(usuario.rolCodigo());
        boolean controlador = "CONTROLADOR".equalsIgnoreCase(usuario.rolCodigo());

        if (controlador) {
            auth.exigirPlazaAsignada(usuario);
        }

        if (!supervisor && !controlador) {
            auth.exigirPlazaAsignada(usuario);

            if (plazaId != null) {
                auth.exigirPuedeConsultarPlaza(usuario, plazaId);
            }

            filtro = usuario.plazaId();
        }

        return queryPort.buscar(
                filtro,
                buscar
        );
    }
}
