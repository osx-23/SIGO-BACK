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
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.UUID;

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

    public record MaterialLote(Long productoId, BigDecimal cantidad) {}
    public record LoteRequest(UUID grupoId, Long origenPlazaId, Long destinoPlazaId,
                              List<MaterialLote> items, String observacion) {}

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
            WITH ultimos_grupos AS (
              SELECT grupo_id,MAX(enviado_en) AS fecha
              FROM inventario_traslado
              WHERE (CAST(:plaza AS BIGINT) IS NULL OR origen_plaza_id=:plaza OR destino_plaza_id=:plaza)
              GROUP BY grupo_id ORDER BY fecha DESC LIMIT 300
            )
            SELECT t.id,t.grupo_id AS "grupoId",
                   t.origen_plaza_id AS "origenPlazaId",
                   o.codigo AS "origen", t.destino_plaza_id AS "destinoPlazaId",
                   d.codigo AS "destino", t.producto_id AS "productoId",
                   pr.nombre AS "producto",pr.unidad_medida AS "unidad",
                   t.cantidad,t.estado,t.observacion,
                   t.enviado_en AS "enviadoEn",t.recibido_en AS "recibidoEn",
                   t.enviado_por AS "enviadoPor",t.recibido_por AS "recibidoPor"
            FROM inventario_traslado t
            JOIN ultimos_grupos g ON g.grupo_id=t.grupo_id
            JOIN plazas o ON o.id=t.origen_plaza_id
            JOIN plazas d ON d.id=t.destino_plaza_id
            JOIN inventario_producto pr ON pr.id=t.producto_id
            ORDER BY g.fecha DESC,t.id ASC
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

    /**
     * Crea un envío de uno o varios materiales en una sola transacción.
     * Usa el mismo bloqueo por plaza/producto del endpoint individual para evitar sobregiros.
     * grupoId suministrado por el cliente evita duplicaciones tras reintentos de red.
     */
    @PostMapping("/lotes")
    @Transactional
    public Map<String, Object> enviarLote(@RequestBody LoteRequest request) {
        InventarioUsuarioActual actual = usuarios.obtenerActual();
        if (request == null || request.origenPlazaId() == null
                || request.destinoPlazaId() == null || request.items() == null
                || request.items().isEmpty() || request.items().size() > 50) {
            throw error(HttpStatus.BAD_REQUEST, "Indique origen, destino y entre 1 y 50 materiales");
        }
        if (request.origenPlazaId().equals(request.destinoPlazaId())) {
            throw error(HttpStatus.BAD_REQUEST, "Origen y destino deben ser diferentes");
        }
        if (request.observacion() != null && request.observacion().length() > 1000) {
            throw error(HttpStatus.BAD_REQUEST, "La observación es demasiado larga");
        }
        permisos.exigirPuedeAdministrarPlaza(actual, request.origenPlazaId());

        // TreeMap garantiza siempre el mismo orden de bloqueo entre operaciones concurrentes.
        TreeMap<Long, BigDecimal> cantidades = new TreeMap<>();
        for (MaterialLote item : request.items()) {
            if (item == null || item.productoId() == null || item.productoId() <= 0
                    || item.cantidad() == null || item.cantidad().signum() <= 0
                    || item.cantidad().stripTrailingZeros().scale() > 2) {
                throw error(HttpStatus.BAD_REQUEST, "Cantidad o material inválido en el envío");
            }
            if (cantidades.putIfAbsent(item.productoId(), item.cantidad()) != null) {
                throw error(HttpStatus.BAD_REQUEST, "No se puede repetir un material dentro del mismo envío");
            }
        }

        UUID grupoId = request.grupoId() == null ? UUID.randomUUID() : request.grupoId();
        var params = p("grupo", grupoId, "origen", request.origenPlazaId(),
                "destino", request.destinoPlazaId(), "usuario", actual.trabajadorId(),
                "observacion", request.observacion());

        // Bloqueo compartido por grupo para admitir reintentos idempotentes.
        jdbc.queryForObject("""
            SELECT pg_advisory_xact_lock(
              hashtext('sigo-traslado-grupo-' || CAST(:grupo AS TEXT))::bigint)
            """, params, (rs, fila) -> 1);
        List<Map<String, Object>> existentes = jdbc.queryForList("""
            SELECT producto_id,cantidad,origen_plaza_id,destino_plaza_id,enviado_por
            FROM inventario_traslado WHERE grupo_id=:grupo
            """, params);
        if (!existentes.isEmpty()) {
            boolean iguales = existentes.size() == cantidades.size();
            for (var fila : existentes) {
                Long id = ((Number) fila.get("producto_id")).longValue();
                BigDecimal cantidad = cantidades.get(id);
                iguales = iguales && cantidad != null
                    && cantidad.compareTo((BigDecimal) fila.get("cantidad")) == 0
                    && request.origenPlazaId().equals(((Number) fila.get("origen_plaza_id")).longValue())
                    && request.destinoPlazaId().equals(((Number) fila.get("destino_plaza_id")).longValue())
                    && actual.trabajadorId().equals(((Number) fila.get("enviado_por")).longValue());
            }
            if (!iguales) throw error(HttpStatus.CONFLICT, "Este identificador ya corresponde a otro envío");
            return Map.of("grupoId", grupoId.toString(), "materiales", existentes.size(),
                    "reutilizado", true);
        }

        Integer existeDestino = jdbc.queryForObject(
                "SELECT COUNT(*) FROM plazas WHERE id=:destino", params, Integer.class);
        if (existeDestino == null || existeDestino == 0) {
            throw error(HttpStatus.BAD_REQUEST, "La plaza de destino no existe");
        }

        // Primero bloquea y comprueba TODOS los materiales. Si alguno falla, no sale ninguno.
        for (var item : cantidades.entrySet()) {
            params.addValue("producto", item.getKey());
            jdbc.queryForObject("""
                SELECT pg_advisory_xact_lock(
                  hashtext('sigo-traslado-' || CAST(:origen AS TEXT) || '-' || CAST(:producto AS TEXT))::bigint)
                """, params, (rs, fila) -> 1);
            List<Map<String, Object>> stocks = jdbc.queryForList("""
                SELECT cantidad_actual,origen_stock FROM v_inventario_stock_actual
                WHERE plaza_id=:origen AND producto_id=:producto
                """, params);
            if (stocks.isEmpty() || "SIN_REGISTRO".equals(stocks.get(0).get("origen_stock"))) {
                throw error(HttpStatus.CONFLICT, "El material #" + item.getKey()
                        + " no tiene stock registrado en la plaza de origen");
            }
            BigDecimal disponible = (BigDecimal) stocks.get(0).get("cantidad_actual");
            if (disponible.compareTo(item.getValue()) < 0) {
                throw error(HttpStatus.CONFLICT, "Stock insuficiente para el material #"
                        + item.getKey() + ": disponible " + disponible);
            }
        }

        List<Long> ids = new ArrayList<>();
        for (var item : cantidades.entrySet()) {
            params.addValue("producto", item.getKey());
            params.addValue("cantidad", item.getValue());
            jdbc.update("""
                INSERT INTO inventario_producto_plaza(producto_id,plaza_id,stock_minimo)
                VALUES (:producto,:destino,0)
                ON CONFLICT (producto_id,plaza_id) DO NOTHING
                """, params);
            Long id = jdbc.queryForObject("""
                INSERT INTO inventario_traslado
                 (grupo_id,origen_plaza_id,destino_plaza_id,producto_id,
                  cantidad,observacion,enviado_por)
                VALUES (:grupo,:origen,:destino,:producto,:cantidad,:observacion,:usuario)
                RETURNING id
                """, params, Long.class);
            ids.add(id);
        }
        return Map.of("grupoId", grupoId.toString(), "materiales", ids.size(),
                "ids", ids, "reutilizado", false);
    }

    /** Confirma todos los materiales de un grupo; los registros históricos siguen siendo grupos de uno. */
    @PostMapping("/lotes/{grupoId}/recibir")
    @Transactional
    public Map<String, Object> recibirLote(@PathVariable UUID grupoId) {
        InventarioUsuarioActual actual = usuarios.obtenerActual();
        var params = p("grupo", grupoId, "usuario", actual.trabajadorId());
        List<Map<String, Object>> filas = jdbc.queryForList("""
            SELECT destino_plaza_id,estado FROM inventario_traslado
            WHERE grupo_id=:grupo ORDER BY id FOR UPDATE
            """, params);
        if (filas.isEmpty()) throw error(HttpStatus.NOT_FOUND, "Envío no encontrado");
        Long destino = ((Number) filas.get(0).get("destino_plaza_id")).longValue();
        permisos.exigirPuedeAdministrarPlaza(actual, destino);
        for (var fila : filas) {
            if (!destino.equals(((Number) fila.get("destino_plaza_id")).longValue())) {
                throw error(HttpStatus.CONFLICT, "El grupo tiene destinos diferentes");
            }
            if (!"EN_TRANSITO".equals(fila.get("estado"))) {
                throw error(HttpStatus.CONFLICT, "El envío ya se recibió o tiene un estado diferente");
            }
        }
        int recibidos = jdbc.update("""
            UPDATE inventario_traslado
            SET estado='RECIBIDO',recibido_en=now(),recibido_por=:usuario
            WHERE grupo_id=:grupo AND estado='EN_TRANSITO'
            """, params);
        if (recibidos != filas.size()) throw error(HttpStatus.CONFLICT, "Recepción incompleta, reintente");
        return Map.of("grupoId", grupoId.toString(), "materialesRecibidos", recibidos,
                      "estado", "RECIBIDO");
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
