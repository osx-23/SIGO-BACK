package com.sigo.inventario.api.controller;

import com.sigo.inventario.api.dto.response.StockActualResponse;
import com.sigo.inventario.application.port.in.InventarioStockUseCase;
import com.sigo.inventario.application.security.InventarioUsuarioActual;
import com.sigo.inventario.application.port.in.ObtenerInventarioUsuarioUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/stock")
@RequiredArgsConstructor
public class InventarioStockController {

    private final InventarioStockUseCase useCase;
    private final ObtenerInventarioUsuarioUseCase usuarios;

    @GetMapping
    public List<StockActualResponse> consultar(
            @RequestParam(required = false) Long plazaId,
            @RequestParam(required = false) String buscar
    ) {
        InventarioUsuarioActual actual =
                usuarios.obtenerActual();

        exigirGestion(actual);

        return useCase
                .consultar(actual, plazaId, buscar)
                .stream()
                .map(stock ->
                        new StockActualResponse(
                                stock.plazaId(),
                                stock.plaza(),
                                stock.productoId(),
                                stock.producto(),
                                stock.unidadMedida(),
                                stock.cantidadActual(),
                                stock.stockMinimo(),
                                stock.bajoMinimo(),
                                stock.inventarioId(),
                                stock.actualizadoEn()
                        )
                )
                .toList();
    }

    private void exigirGestion(
            InventarioUsuarioActual actual
    ) {
        if (!actual.esControladorSistema()
                && !actual.esSupervisorSistema()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo controladores y supervisores pueden consultar el stock de inventario"
            );
        }
    }
}
