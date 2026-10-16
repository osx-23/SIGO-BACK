package com.sigo.inventario.api.controller;

import com.sigo.inventario.application.port.in.ObtenerInventarioUsuarioUseCase;
import com.sigo.inventario.application.security.InventarioAuthorizationService;
import com.sigo.inventario.application.security.InventarioUsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventario/traslados")
@PreAuthorize("hasAnyRole('SUPERVISOR','CONTROLADOR')")
@RequiredArgsConstructor
public class InventarioTrasladosController {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObtenerInventarioUsuarioUseCase usuarios;
    private final InventarioAuthorizationService permisos;

    public record TrasladoRequest(Long origenPlazaId, Long destinoPlazaId,
                                  Long productoId, BigDecimal cantidad, String observacion) {}

    private MapSqlParameterSource p(Object... pares) {
        var params = new MapSqlParameterSource();
        for (int i = 0; i < pares.length; i += 2) {
            params.addValue((String) pares[i], pares[i + 1]);
        }
        return params;
    }

    private ResponseStatusException error(HttpStatus status, String mensaje) {
        return new ResponseStatusException(status, mensaje);
    }

    @GetMapping
    public List<Map<String, Object>> listar(@RequestParam(required = false) Long plazaId) {
        InventarioUsuarioActual actual = usuarios.obtenerActual();
        if (plazaId != null) permisos.exigirPuedeConsultarPlaza(actual, plazaId);
        Long filtro = permisos.esSupervisor(actual) ? plazaId : actual.plazaId();
        if (filtro == null && !permisos.esSupervisor(actual)) {
            throw error(HttpStatus.FORBIDDEN, "No tiene plaza asignada");
        }
        return jdbc.queryForList("""
            SELECT t.id, t.origen_plaza_id AS "origenPlazaId",
                   o.codigo AS "origen", t.destino_plaza_id AS "destinoPlazaId",
                   d.codigo AS "destino", t.producto_id AS "productoId",
                   pr.nombre AS "producto",pr.unidad_medida AS "unidad",
                   t.cantidad,t.estado,t.observacion,
                   t.enviado_en AS "enviadoEn",t.recibido_en AS "recibidoEn",
                   t.enviado_por AS "enviadoPor",t.recibido_por AS "recibidoPor"
            FROM inventario_traslado t
            JOIN plazas o ON o.id=t.origen_plaza_id
            JOIN plazas d ON d.id=t.destino_plaza_id
            JOIN inventario_producto pr ON pr.id=t.producto_id
            WHERE (CAST(:plaza AS BIGINT) IS NULL OR t.origen_plaza_id=:plaza OR t.destino_plaza_id=:plaza)
            ORDER BY t.enviado_en DESC,t.id DESC LIMIT 300
            """, p("plaza", filtro));
    }

    @PostMapping
    @Transactional
    public Map<String, Object> enviar(@RequestBody TrasladoRequest request) {
        InventarioUsuarioActual actual = usuarios.obtenerActual();
        if (request == null || request.origenPlazaId() == null
                || request.destinoPlazaId() == null || request.productoId() == null
                || request.cantidad() == null) {
            throw error(HttpStatus.BAD_REQUEST, "Debe seleccionar origen, destino, material y cantidad");
        }
        if (request.origenPlazaId().equals(request.destinoPlazaId())) {
            throw error(HttpStatus.BAD_REQUEST, "La plaza de destino debe ser diferente");
        }
        if (request.cantidad().signum() <= 0 || request.cantidad().scale() > 2) {
            throw error(HttpStatus.BAD_REQUEST, "Ingrese una cantidad positiva con máximo dos decimales");
        }
        if (request.observacion() != null && request.observacion().length() > 1000) {
            throw error(HttpStatus.BAD_REQUEST, "La observación es demasiado larga");
        }

        permisos.exigirPuedeAdministrarPlaza(actual, request.origenPlazaId());
        var params = p("origen", request.origenPlazaId(), "destino", request.destinoPlazaId(),
                "producto", request.productoId(), "cantidad", request.cantidad(),
                "observacion", request.observacion(), "usuario", actual.trabajadorId());

        Integer destinoExiste = jdbc.queryForObject(
                "SELECT COUNT(*) FROM plazas WHERE id=:destino", params, Integer.class);
        if (destinoExiste == null || destinoExiste == 0) {
            throw error(HttpStatus.BAD_REQUEST, "La plaza de destino no existe");
        }

        // Se serializan las salidas del mismo producto/plaza para impedir sobregiros.
        jdbc.queryForObject("""
            SELECT pg_advisory_xact_lock(
              hashtext('sigo-traslado-' || CAST(:origen AS TEXT) || '-' || CAST(:producto AS TEXT))::bigint)
            """, params, (rs, row) -> 1);

        List<Map<String, Object>> stocks = jdbc.queryForList("""
            SELECT cantidad_actual,origen_stock FROM v_inventario_stock_actual
            WHERE plaza_id=:origen AND producto_id=:producto
            """, params);
        if (stocks.isEmpty() || "SIN_REGISTRO".equals(String.valueOf(stocks.get(0).get("origen_stock")))) {
            throw error(HttpStatus.CONFLICT, "El material no tiene stock registrado en la plaza de origen");
        }
        BigDecimal disponible = (BigDecimal) stocks.get(0).get("cantidad_actual");
        if (disponible.compareTo(request.cantidad()) < 0) {
            throw error(HttpStatus.CONFLICT, "Stock insuficiente: disponible " + disponible);
        }

        // La plaza receptora podrá ver el material aunque no tuviera un conteo previo.
        jdbc.update("""
            INSERT INTO inventario_producto_plaza(producto_id,plaza_id,stock_minimo)
            VALUES (:producto,:destino,0)
            ON CONFLICT (producto_id,plaza_id) DO NOTHING
            """, params);

        Long id = jdbc.queryForObject("""
            INSERT INTO inventario_traslado
             (origen_plaza_id,destino_plaza_id,producto_id,cantidad,observacion,enviado_por)
            VALUES (:origen,:destino,:producto,:cantidad,:observacion,:usuario)
            RETURNING id
            """, params, Long.class);
        return detalle(id);
    }

    @PostMapping("/{id}/recibir")
    @Transactional
    public Map<String, Object> recibir(@PathVariable Long id) {
        InventarioUsuarioActual actual = usuarios.obtenerActual();
        var params = p("id", id, "usuario", actual.trabajadorId());
        List<Map<String, Object>> filas = jdbc.queryForList("""
            SELECT destino_plaza_id,estado FROM inventario_traslado WHERE id=:id FOR UPDATE
            """, params);
        if (filas.isEmpty()) throw error(HttpStatus.NOT_FOUND, "Traslado no encontrado");
        Long destino = ((Number) filas.get(0).get("destino_plaza_id")).longValue();
        permisos.exigirPuedeAdministrarPlaza(actual, destino);
        if (!"EN_TRANSITO".equals(filas.get(0).get("estado"))) {
            throw error(HttpStatus.CONFLICT, "El traslado ya fue recibido");
        }
        jdbc.update("""
            UPDATE inventario_traslado
            SET estado='RECIBIDO',recibido_en=now(),recibido_por=:usuario
            WHERE id=:id AND estado='EN_TRANSITO'
            """, params);
        return detalle(id);
    }

    private Map<String, Object> detalle(Long id) {
        return jdbc.queryForMap("""
            SELECT id,origen_plaza_id AS "origenPlazaId",
                   destino_plaza_id AS "destinoPlazaId",producto_id AS "productoId",
                   cantidad,estado,enviado_en AS "enviadoEn",recibido_en AS "recibidoEn"
            FROM inventario_traslado WHERE id=:id
            """, p("id", id));
    }
}
