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
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Pedidos mensuales y recepciones por plaza.
 * Los conteos finalizados siguen siendo la base del stock; cada recepción
 * posterior se suma una sola vez mediante v_inventario_stock_actual.
 */
@RestController
@RequestMapping("/api/inventario/pedidos")
@PreAuthorize("hasAnyRole('SUPERVISOR','CONTROLADOR')")
@RequiredArgsConstructor
public class InventarioPedidosController {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObtenerInventarioUsuarioUseCase usuarios;
    private final InventarioAuthorizationService permisos;

    public record ItemPedido(Long productoId, BigDecimal cantidad, BigDecimal sugerida,
                             BigDecimal promedio, BigDecimal stock) {}
    public record PedidoRequest(Long plazaId, String periodo, String observacion,
                                List<ItemPedido> items) {}
    public record ItemRecepcion(Long productoId, BigDecimal cantidad) {}
    public record RecepcionRequest(UUID claveIdempotencia, String observacion,
                                   List<ItemRecepcion> items) {}

    private InventarioUsuarioActual usuario() { return usuarios.obtenerActual(); }

    private void supervisor(InventarioUsuarioActual u) {
        if (!permisos.esSupervisor(u))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el supervisor puede aprobar o consolidar pedidos");
    }
    private void verPlaza(InventarioUsuarioActual u, Long plazaId) {
        permisos.exigirPuedeConsultarPlaza(u, plazaId);
    }
    private void gestionarPlaza(InventarioUsuarioActual u, Long plazaId) {
        permisos.exigirPuedeAdministrarPlaza(u, plazaId);
    }
    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }
    private LocalDate periodo(String valor) {
        try { return YearMonth.parse(valor).atDay(1); }
        catch (Exception e) { throw error(HttpStatus.BAD_REQUEST, "Periodo inválido, use AAAA-MM"); }
    }
    private Long plazaSolicitada(InventarioUsuarioActual u, Long plazaId) {
        Long plaza = plazaId != null ? plazaId : u.plazaId();
        if (plaza == null) throw error(HttpStatus.BAD_REQUEST, "Seleccione una plaza");
        gestionarPlaza(u, plaza);
        return plaza;
    }
    private MapSqlParameterSource parametros(Object... pares) {
        var p = new MapSqlParameterSource();
        for (int i=0; i<pares.length; i+=2) p.addValue((String) pares[i], pares[i+1]);
        return p;
    }

    @GetMapping("/consumo")
    public List<Map<String,Object>> consumo(@RequestParam(required=false) Long plazaId) {
        var u=usuario();
        Long plaza=plazaSolicitada(u,plazaId);
        return jdbc.queryForList("""
            SELECT h.plaza_id AS "plazaId",h.producto_id AS "productoId",
                   pr.nombre AS producto,TO_CHAR(h.periodo,'YYYY-MM') AS periodo,
                   h.consumo
            FROM v_inventario_consumo_mensual h
            JOIN inventario_producto pr ON pr.id=h.producto_id
            WHERE h.plaza_id=:plaza
            ORDER BY h.periodo DESC,pr.nombre LIMIT 500
            """,parametros("plaza",plaza));
    }

    @GetMapping("/sugerencias")
    public List<Map<String,Object>> sugerencias(@RequestParam(required=false) Long plazaId,
                                                 @RequestParam String periodo) {
        var u = usuario();
        Long plaza = plazaSolicitada(u, plazaId);
        LocalDate mes = periodo(periodo);
        return jdbc.queryForList("""
            SELECT s."productoId",s."producto",s."unidadMedida",s."stockActual",
                   s."stockMinimo", h.promedio AS "promedioMensual",
                   CASE WHEN h.promedio IS NULL THEN NULL
                   ELSE GREATEST(0, CEIL(h.promedio - s."stockActual")) END AS "sugerido",
                   (h.promedio IS NOT NULL) AS "conHistorico"
            FROM (
              SELECT producto_id AS "productoId",producto,unidad_medida AS "unidadMedida",
                     cantidad_actual AS "stockActual",
                     COALESCE(pp.stock_minimo,0) AS "stockMinimo"
              FROM v_inventario_stock_actual v
              JOIN inventario_producto_plaza pp ON pp.producto_id=v.producto_id AND pp.plaza_id=v.plaza_id
              JOIN inventario_producto pr ON pr.id=v.producto_id
              WHERE v.plaza_id=:plaza AND pr.activo=true
            ) s
            LEFT JOIN (
              SELECT producto_id,ROUND(SUM(consumo)/NULLIF(COUNT(DISTINCT periodo),0),2) promedio
              FROM v_inventario_consumo_mensual
              WHERE plaza_id=:plaza AND periodo>=CAST(:mes AS DATE)-INTERVAL '6 months'
                AND periodo<CAST(:mes AS DATE)
              GROUP BY producto_id
            ) h ON h.producto_id=s."productoId"
            ORDER BY s."producto"
            """, parametros("plaza",plaza,"mes",mes));
    }

    @GetMapping
    public List<Map<String,Object>> listar(@RequestParam(required=false) Long plazaId,
                                            @RequestParam(required=false) String periodo) {
        var u=usuario();
        if (plazaId!=null) verPlaza(u,plazaId);
        Long filtro = permisos.esSupervisor(u) ? plazaId : u.plazaId();
        if (filtro==null && !permisos.esSupervisor(u))
            throw error(HttpStatus.FORBIDDEN,"El usuario no tiene plaza asignada");
        return jdbc.queryForList("""
            SELECT ped.id, ped.plaza_id AS "plazaId", p.codigo AS plaza,
                   TO_CHAR(ped.periodo,'YYYY-MM') AS periodo,
                   ped.estado,ped.observacion,ped.creado_en AS "creadoEn",
                   COALESCE(SUM(d.cantidad_solicitada),0) AS "totalSolicitado",
                   COALESCE((SELECT SUM(rd.cantidad) FROM inventario_pedido_recepcion_detalle rd
                     JOIN inventario_pedido_recepcion r ON r.id=rd.recepcion_id
                     WHERE r.pedido_id=ped.id),0) AS "totalRecibido"
            FROM inventario_pedido ped
            JOIN plazas p ON p.id=ped.plaza_id
            LEFT JOIN inventario_pedido_detalle d ON d.pedido_id=ped.id
            WHERE (:plaza IS NULL OR ped.plaza_id=:plaza)
              AND (:mes IS NULL OR ped.periodo=CAST(:mes AS DATE))
            GROUP BY ped.id,p.codigo
            ORDER BY ped.periodo DESC,p.codigo
            """, parametros("plaza",filtro,"mes",periodo==null?null:periodo(periodo)));
    }

    @GetMapping("/{id}")
    public Map<String,Object> detalle(@PathVariable Long id) {
        var u=usuario();
        Map<String,Object> ped=pedido(id,false);
        verPlaza(u, ((Number)ped.get("plazaId")).longValue());
        ped.put("items", jdbc.queryForList("""
            SELECT d.producto_id AS "productoId",p.nombre AS producto,p.unidad_medida AS unidad,
                   d.cantidad_solicitada AS cantidad,d.cantidad_sugerida AS sugerida,
                   d.promedio_mensual AS promedio,d.stock_referencia AS stock,
                   COALESCE(SUM(rd.cantidad),0) AS recibida,
                   GREATEST(0,d.cantidad_solicitada-COALESCE(SUM(rd.cantidad),0)) AS pendiente
            FROM inventario_pedido_detalle d
            JOIN inventario_producto p ON p.id=d.producto_id
            LEFT JOIN inventario_pedido_recepcion r ON r.pedido_id=d.pedido_id
            LEFT JOIN inventario_pedido_recepcion_detalle rd
              ON rd.recepcion_id=r.id AND rd.producto_id=d.producto_id
            WHERE d.pedido_id=:id GROUP BY d.id,p.nombre,p.unidad_medida
            ORDER BY p.nombre
            """,parametros("id",id)));
        ped.put("recepciones",jdbc.queryForList("""
            SELECT r.id,r.creado_en AS fecha,r.observacion,r.recibido_por AS "recibidoPor",
                   p.nombre AS producto,rd.producto_id AS "productoId",rd.cantidad
            FROM inventario_pedido_recepcion r
            JOIN inventario_pedido_recepcion_detalle rd ON rd.recepcion_id=r.id
            JOIN inventario_producto p ON p.id=rd.producto_id
            WHERE r.pedido_id=:id ORDER BY r.id DESC,p.nombre
            """,parametros("id",id)));
        return ped;
    }

    private Map<String,Object> pedido(Long id,boolean bloquear) {
        String sql="""
            SELECT ped.id,ped.plaza_id AS "plazaId",p.codigo AS plaza,
                   TO_CHAR(ped.periodo,'YYYY-MM') AS periodo,
                   ped.estado,ped.observacion,ped.creado_por AS "creadoPor",
                   ped.aprobado_por AS "aprobadoPor",ped.creado_en AS "creadoEn",
                   ped.aprobado_en AS "aprobadoEn",ped.enviado_en AS "enviadoEn"
            FROM inventario_pedido ped JOIN plazas p ON p.id=ped.plaza_id
            WHERE ped.id=:id
            """+(bloquear?" FOR UPDATE OF ped":"");
        List<Map<String,Object>> filas=jdbc.queryForList(sql,parametros("id",id));
        if(filas.isEmpty()) throw error(HttpStatus.NOT_FOUND,"Pedido no encontrado");
        return new LinkedHashMap<>(filas.getFirst());
    }
    private void estado(Map<String,Object> p,String... permitidos) {
        if(!Arrays.asList(permitidos).contains(p.get("estado")))
            throw error(HttpStatus.CONFLICT,"El pedido está en estado "+p.get("estado"));
    }
    private void validarItems(List<ItemPedido> items,Long plazaId) {
        if(items==null || items.isEmpty() || items.size()>500)
            throw error(HttpStatus.BAD_REQUEST,"Agregue entre 1 y 500 materiales");
        Set<Long> ids=new HashSet<>();
        boolean alguno=false;
        for(var i:items) {
            if(i==null || i.productoId()==null || !ids.add(i.productoId())
                || i.cantidad()==null || i.cantidad().signum()<0 || i.cantidad().scale()>2)
                throw error(HttpStatus.BAD_REQUEST,"Productos duplicados o cantidades inválidas");
            if(i.cantidad().signum()>0) alguno=true;
            for(var valor:List.of(i.cantidad(),i.sugerida()==null?BigDecimal.ZERO:i.sugerida(),
                    i.promedio()==null?BigDecimal.ZERO:i.promedio(),i.stock()==null?BigDecimal.ZERO:i.stock())) {
                if(valor.signum()<0 || valor.scale()>2 || valor.precision()>14)
                    throw error(HttpStatus.BAD_REQUEST,"Cantidad fuera de rango");
            }
            Integer n=jdbc.queryForObject("""
                SELECT COUNT(*) FROM inventario_producto_plaza pp
                JOIN inventario_producto p ON p.id=pp.producto_id
                WHERE pp.plaza_id=:plaza AND pp.producto_id=:producto AND p.activo=true
                """,parametros("plaza",plazaId,"producto",i.productoId()),Integer.class);
            if(n==null || n==0) throw error(HttpStatus.BAD_REQUEST,
                    "El material "+i.productoId()+" no está habilitado para la plaza");
        }
        if(!alguno) throw error(HttpStatus.BAD_REQUEST,"Solicite al menos un material");
    }
    private void guardarItems(Long pedidoId,List<ItemPedido> items) {
        for(var i:items) jdbc.update("""
            INSERT INTO inventario_pedido_detalle
              (pedido_id,producto_id,cantidad_solicitada,cantidad_sugerida,promedio_mensual,stock_referencia)
            VALUES (:id,:producto,:cantidad,:sugerida,:promedio,:stock)
            """,parametros("id",pedidoId,"producto",i.productoId(),"cantidad",i.cantidad(),
            "sugerida",i.sugerida(),"promedio",i.promedio(),"stock",i.stock()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String,Object> crear(@RequestBody PedidoRequest solicitud) {
        var u=usuario();
        if(solicitud==null) throw error(HttpStatus.BAD_REQUEST,"Solicitud vacía");
        Long plaza=plazaSolicitada(u,solicitud.plazaId());
        LocalDate mes=periodo(solicitud.periodo());
        validarItems(solicitud.items(),plaza);
        Integer ya=jdbc.queryForObject("""
            SELECT COUNT(*) FROM inventario_pedido WHERE plaza_id=:plaza AND periodo=:mes
            """,parametros("plaza",plaza,"mes",mes),Integer.class);
        if(ya!=null && ya>0) throw error(HttpStatus.CONFLICT,"Ya existe un pedido de esa plaza y mes");
        Long id=jdbc.queryForObject("""
            INSERT INTO inventario_pedido(plaza_id,periodo,creado_por,observacion)
            VALUES(:plaza,:mes,:creador,:nota) RETURNING id
            """,parametros("plaza",plaza,"mes",mes,"creador",u.trabajadorId(),
            "nota",solicitud.observacion()),Long.class);
        guardarItems(id,solicitud.items());
        return detalle(id);
    }

    @PutMapping("/{id}")
    @Transactional
    public Map<String,Object> editar(@PathVariable Long id,@RequestBody PedidoRequest solicitud) {
        var u=usuario();
        var ped=pedido(id,true);
        Long plaza=((Number)ped.get("plazaId")).longValue();
        gestionarPlaza(u,plaza);
        estado(ped,"BORRADOR","RECHAZADO");
        if(solicitud==null) throw error(HttpStatus.BAD_REQUEST,"Solicitud vacía");
        validarItems(solicitud.items(),plaza);
        jdbc.update("DELETE FROM inventario_pedido_detalle WHERE pedido_id=:id",parametros("id",id));
        guardarItems(id,solicitud.items());
        jdbc.update("""
            UPDATE inventario_pedido SET estado='BORRADOR',observacion=:nota,
            actualizado_en=now() WHERE id=:id
            """,parametros("id",id,"nota",solicitud.observacion()));
        return detalle(id);
    }

    @PostMapping("/{id}/enviar")
    @Transactional
    public Map<String,Object> enviar(@PathVariable Long id) {
        var u=usuario();
        var ped=pedido(id,true);
        gestionarPlaza(u,((Number)ped.get("plazaId")).longValue());
        estado(ped,"BORRADOR","RECHAZADO");
        jdbc.update("""
            UPDATE inventario_pedido SET estado='PENDIENTE',actualizado_en=now()
            WHERE id=:id
            """,parametros("id",id));
        return detalle(id);
    }

    @PostMapping("/{id}/aprobar")
    @Transactional
    public Map<String,Object> aprobar(@PathVariable Long id) {
        var u=usuario(); supervisor(u);
        var ped=pedido(id,true);
        estado(ped,"PENDIENTE");
        jdbc.update("""
            UPDATE inventario_pedido
            SET estado='APROBADO',aprobado_por=:usuario,aprobado_en=now(),actualizado_en=now()
            WHERE id=:id
            """,parametros("id",id,"usuario",u.trabajadorId()));
        return detalle(id);
    }

    @PostMapping("/{id}/rechazar")
    @Transactional
    public Map<String,Object> rechazar(@PathVariable Long id) {
        supervisor(usuario());
        var ped=pedido(id,true);
        estado(ped,"PENDIENTE");
        jdbc.update("""
            UPDATE inventario_pedido SET estado='RECHAZADO',actualizado_en=now() WHERE id=:id
            """,parametros("id",id));
        return detalle(id);
    }

    @GetMapping("/consolidado")
    public List<Map<String,Object>> consolidado(@RequestParam String periodo) {
        supervisor(usuario());
        return jdbc.queryForList("""
            SELECT d.producto_id AS "productoId",pr.nombre AS producto,
                   pr.unidad_medida AS unidad,
                   SUM(d.cantidad_solicitada) AS "cantidadTotal",
                   COUNT(DISTINCT ped.plaza_id) AS plazas
            FROM inventario_pedido ped
            JOIN inventario_pedido_detalle d ON d.pedido_id=ped.id
            JOIN inventario_producto pr ON pr.id=d.producto_id
            WHERE ped.periodo=CAST(:mes AS DATE) AND ped.estado IN
              ('APROBADO','ENVIADO','PARCIAL','COMPLETADO','CERRADO_FALTANTES')
            GROUP BY d.producto_id,pr.nombre,pr.unidad_medida ORDER BY pr.nombre
            """,parametros("mes",periodo(periodo)));
    }

    @PostMapping("/consolidar")
    @Transactional
    public List<Map<String,Object>> consolidar(@RequestParam String periodo) {
        supervisor(usuario());
        LocalDate mes=periodo(periodo);
        jdbc.update("""
            UPDATE inventario_pedido SET estado='ENVIADO',enviado_en=now(),
            actualizado_en=now()
            WHERE periodo=:mes AND estado='APROBADO'
            """,parametros("mes",mes));
        return consolidado(periodo);
    }

    @PostMapping("/{id}/recepciones")
    @Transactional
    public Map<String,Object> recibir(@PathVariable Long id,@RequestBody RecepcionRequest solicitud) {
        var u=usuario();
        if(solicitud==null || solicitud.claveIdempotencia()==null ||
            solicitud.items()==null || solicitud.items().isEmpty())
            throw error(HttpStatus.BAD_REQUEST,"Indique la clave de recepción y los materiales recibidos");
        var ped=pedido(id,true); // bloqueo: serializa recepciones concurrentes
        gestionarPlaza(u,((Number)ped.get("plazaId")).longValue());
        // Misma clave = mismo evento. La repetición no vuelve a sumar.
        Integer repetido=jdbc.queryForObject("""
            SELECT COUNT(*) FROM inventario_pedido_recepcion
            WHERE pedido_id=:id AND clave_idempotencia=:clave
            """,parametros("id",id,"clave",solicitud.claveIdempotencia()),Integer.class);
        if(repetido!=null && repetido>0) return detalle(id);
        estado(ped,"ENVIADO","APROBADO","PARCIAL");
        Map<Long,BigDecimal> faltantes=new HashMap<>();
        for(var fila:jdbc.queryForList("""
            SELECT d.producto_id,GREATEST(0,d.cantidad_solicitada-
              COALESCE(SUM(rd.cantidad),0)) AS pendiente
            FROM inventario_pedido_detalle d
            LEFT JOIN inventario_pedido_recepcion r ON r.pedido_id=d.pedido_id
            LEFT JOIN inventario_pedido_recepcion_detalle rd
              ON rd.recepcion_id=r.id AND rd.producto_id=d.producto_id
            WHERE d.pedido_id=:id GROUP BY d.id
            """,parametros("id",id))) {
            faltantes.put(((Number)fila.get("producto_id")).longValue(),
                          (BigDecimal)fila.get("pendiente"));
        }
        Set<Long> usados=new HashSet<>();
        for(var i:solicitud.items()) {
            if(i==null || i.productoId()==null || i.cantidad()==null ||
               i.cantidad().signum()<=0 || i.cantidad().scale()>2 ||
               i.cantidad().precision()>14 || !usados.add(i.productoId()) ||
               !faltantes.containsKey(i.productoId()) ||
               i.cantidad().compareTo(faltantes.get(i.productoId()))>0)
                throw error(HttpStatus.BAD_REQUEST,"Recepción inválida o superior a lo pendiente");
            faltantes.put(i.productoId(),faltantes.get(i.productoId()).subtract(i.cantidad()));
        }
        Long recepcionId=jdbc.queryForObject("""
            INSERT INTO inventario_pedido_recepcion
              (pedido_id,clave_idempotencia,recibido_por,observacion)
            VALUES (:id,:clave,:usuario,:nota) RETURNING id
            """,parametros("id",id,"clave",solicitud.claveIdempotencia(),
            "usuario",u.trabajadorId(),"nota",solicitud.observacion()),Long.class);
        for(var i:solicitud.items()) jdbc.update("""
            INSERT INTO inventario_pedido_recepcion_detalle(recepcion_id,producto_id,cantidad)
            VALUES (:r,:producto,:cantidad)
            """,parametros("r",recepcionId,"producto",i.productoId(),"cantidad",i.cantidad()));
        boolean completo=faltantes.values().stream().allMatch(n->n.signum()==0);
        jdbc.update("""
            UPDATE inventario_pedido SET estado=:estado,actualizado_en=now()
            WHERE id=:id
            """,parametros("estado",completo?"COMPLETADO":"PARCIAL","id",id));
        return detalle(id);
    }

    @PostMapping("/{id}/cerrar-faltantes")
    @Transactional
    public Map<String,Object> cerrarFaltantes(@PathVariable Long id) {
        supervisor(usuario());
        var ped=pedido(id,true);
        estado(ped,"PARCIAL","ENVIADO","APROBADO");
        jdbc.update("""
            UPDATE inventario_pedido SET estado='CERRADO_FALTANTES',actualizado_en=now()
            WHERE id=:id
            """,parametros("id",id));
        return detalle(id);
    }
}
