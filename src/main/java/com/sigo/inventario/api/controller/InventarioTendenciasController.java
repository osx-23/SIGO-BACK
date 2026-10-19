package com.sigo.inventario.api.controller;

import com.sigo.inventario.application.port.in.ObtenerInventarioUsuarioUseCase;
import com.sigo.inventario.application.security.InventarioAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventario/stock/tendencia")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPERVISOR','CONTROLADOR')")
public class InventarioTendenciasController {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObtenerInventarioUsuarioUseCase usuarios;
    private final InventarioAuthorizationService permisos;

    @GetMapping
    public List<Map<String, Object>> tendencia(@RequestParam Long plazaId,
                                                @RequestParam Long productoId) {
        if (plazaId <= 0 || productoId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seleccione plaza y material");
        }
        permisos.exigirPuedeConsultarPlaza(usuarios.obtenerActual(), plazaId);
        var params = new MapSqlParameterSource()
                .addValue("plaza", plazaId).addValue("producto", productoId);
        // Cada barra corresponde a un conteo finalizado, no a consumo inferido.
        return jdbc.queryForList("""
            SELECT i.id AS "inventarioId",
                   i.fecha_finalizacion AS fecha,
                   d.cantidad_encontrada AS cantidad
            FROM inventario_conteo i
            JOIN inventario_conteo_detalle d ON d.inventario_id=i.id
            WHERE i.estado='FINALIZADO' AND i.plaza_id=:plaza AND d.producto_id=:producto
              AND i.fecha_finalizacion>=now()-INTERVAL '12 months'
            ORDER BY i.fecha_finalizacion ASC,i.id ASC
            LIMIT 120
            """, params);
    }
}
