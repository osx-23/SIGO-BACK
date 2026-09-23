package com.sigo.chat.service;

import com.sigo.chat.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SigoReadOnlyQueryService {

    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public SigoToolResult ejecutar(SigoToolRequest req) {
        if (req == null || req.tool() == null) {
            return vacio("Consulta", "No se pudo determinar qué información consultar.");
        }

        return switch (req.tool()) {
            case BUSCAR_TRABAJADOR -> buscarTrabajador(req);
            case RESUMEN_ASISTENCIA -> resumenAsistencia(req);
            case AUSENCIAS -> ausencias(req);
            case APOYO_SOLICITADO -> apoyo(req);
            case LISTAR_PLAZAS -> plazas(req);
            case LISTAR_TURNOS -> turnos(req);
            case LISTAR_MOTIVOS -> motivos(req);
            case RELEVOS -> relevos(req);
            case VIAS -> vias(req);
            case EVIDENCIAS_ASISTENCIA -> evidenciasAsistencia(req);
            case EVIDENCIAS_RELEVO -> evidenciasRelevo(req);
            case RESUMEN_GENERAL -> resumenGeneral(req);
            case INCIDENCIAS -> vacio(
                    "Incidencias",
                    "El módulo de incidencias todavía no existe en el backend actual de SIGO."
            );
            case DESCONOCIDO -> vacio(
                    "Consulta",
                    "No pude relacionar la pregunta con información disponible en SIGO."
            );
        };
    }

    private SigoToolResult buscarTrabajador(SigoToolRequest req) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    t.codigo,
                    t.nombre_completo,
                    p.codigo AS plaza,
                    pu.nombre AS puesto,
                    t.activo
                FROM trabajadores t
                LEFT JOIN plazas p ON p.id = t.plaza_id
                LEFT JOIN puestos pu ON pu.id = t.puesto_id
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();

        if (req.codigo() != null) {
            sql.append(" AND t.codigo = ?");
            params.add(req.codigo());
        }
        if (texto(req.trabajador())) {
            sql.append(" AND LOWER(t.nombre_completo) LIKE LOWER(?)");
            params.add("%" + req.trabajador().trim() + "%");
        }
        if (texto(req.plaza())) {
            sql.append(" AND UPPER(p.codigo) = UPPER(?)");
            params.add(req.plaza().trim());
        }

        sql.append(" ORDER BY t.nombre_completo LIMIT ?");
        params.add(req.limiteSeguro());

        return resultado("Trabajadores", query(sql, params), "Información registrada en SIGO.");
    }

    private SigoToolResult resumenAsistencia(SigoToolRequest req) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    COUNT(*) AS registros,
                    COALESCE(SUM(ar.programados), 0) AS programados,
                    COALESCE(SUM(ar.presentes), 0) AS presentes,
                    COALESCE(SUM(ar.apoyo_solicitado), 0) AS apoyo_solicitado,
                    COALESCE(SUM(ar.presentes + ar.apoyo_solicitado), 0) AS operativos,
                    COALESCE(SUM(ar.programados - ar.presentes), 0) AS ausentes,
                    CASE
                        WHEN COALESCE(SUM(ar.programados), 0) = 0 THEN 0
                        ELSE ROUND(
                            SUM(ar.presentes)::numeric /
                            SUM(ar.programados)::numeric * 100,
                            2
                        )
                    END AS porcentaje_asistencia
                FROM asistencia_registro ar
                JOIN plazas p ON p.id = ar.plaza_id
                JOIN turnos tu ON tu.id = ar.turno_id
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();
        agregarFiltrosAsistencia(sql, params, req, "ar", "p", "tu");

        return resultado("Resumen de asistencia", query(sql, params), periodo(req));
    }

    private SigoToolResult ausencias(SigoToolRequest req) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    ar.fecha,
                    p.codigo AS plaza,
                    tu.codigo AS turno,
                    t.codigo AS codigo_trabajador,
                    t.nombre_completo AS trabajador,
                    ma.nombre AS motivo,
                    aa.observacion
                FROM asistencia_ausencia aa
                JOIN asistencia_registro ar ON ar.id = aa.asistencia_id
                JOIN trabajadores t ON t.id = aa.trabajador_id
                JOIN motivos_ausencia ma ON ma.id = aa.motivo_id
                JOIN plazas p ON p.id = ar.plaza_id
                JOIN turnos tu ON tu.id = ar.turno_id
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();

        if (req.codigo() != null) {
            sql.append(" AND t.codigo = ?");
            params.add(req.codigo());
        }
        if (texto(req.trabajador())) {
            sql.append(" AND LOWER(t.nombre_completo) LIKE LOWER(?)");
            params.add("%" + req.trabajador().trim() + "%");
        }
        if (texto(req.plaza())) {
            sql.append(" AND UPPER(p.codigo) = UPPER(?)");
            params.add(req.plaza().trim());
        }
        if (texto(req.turno())) {
            sql.append(" AND UPPER(tu.codigo) = UPPER(?)");
            params.add(req.turno().trim());
        }
        if (texto(req.motivo())) {
            sql.append(" AND LOWER(ma.nombre) LIKE LOWER(?)");
            params.add("%" + req.motivo().trim() + "%");
        }

        agregarFechas(sql, params, req, "ar.fecha");
        sql.append(" ORDER BY ar.fecha DESC, t.nombre_completo LIMIT ?");
        params.add(req.limiteSeguro());

        return resultado("Ausencias", query(sql, params), periodo(req));
    }

    private SigoToolResult apoyo(SigoToolRequest req) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    ar.fecha,
                    p.codigo AS plaza,
                    tu.codigo AS turno,
                    ar.apoyo_solicitado,
                    ar.detalle_apoyo
                FROM asistencia_registro ar
                JOIN plazas p ON p.id = ar.plaza_id
                JOIN turnos tu ON tu.id = ar.turno_id
                WHERE ar.apoyo_solicitado > 0
                """);
        List<Object> params = new ArrayList<>();
        agregarFiltrosAsistencia(sql, params, req, "ar", "p", "tu");
        sql.append(" ORDER BY ar.fecha DESC LIMIT ?");
        params.add(req.limiteSeguro());

        return resultado("Apoyo solicitado", query(sql, params), periodo(req));
    }

    private SigoToolResult plazas(SigoToolRequest req) {
        return resultado(
                "Plazas",
                jdbc.queryForList("SELECT codigo, descripcion, activo FROM plazas ORDER BY codigo LIMIT ?", req.limiteSeguro()),
                null
        );
    }

    private SigoToolResult turnos(SigoToolRequest req) {
        return resultado(
                "Turnos",
                jdbc.queryForList("SELECT codigo, nombre, personal_programado FROM turnos ORDER BY codigo LIMIT ?", req.limiteSeguro()),
                null
        );
    }

    private SigoToolResult motivos(SigoToolRequest req) {
        return resultado(
                "Motivos de ausencia",
                jdbc.queryForList("SELECT nombre FROM motivos_ausencia ORDER BY nombre LIMIT ?", req.limiteSeguro()),
                null
        );
    }

    private SigoToolResult relevos(SigoToolRequest req) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    r.id AS relevo_id,
                    r.fecha,
                    r.hora,
                    p.codigo AS plaza,
                    tu.codigo AS turno,
                    op.codigo AS codigo_operador,
                    op.nombre_completo AS operador,
                    r.observaciones,
                    r.resumen,
                    COUNT(DISTINCT CASE WHEN rc.estado <> 'OPERATIVO' THEN rc.id END) AS elementos_con_observacion,
                    COUNT(DISTINCT CASE WHEN rv.estado <> 'OPERATIVO' THEN rv.id END) AS vias_con_observacion
                FROM relevos r
                JOIN plazas p ON p.id = r.plaza_id
                JOIN turnos tu ON tu.id = r.turno_id
                JOIN trabajadores op ON op.id = r.operador_id
                LEFT JOIN relevo_checklist rc ON rc.relevo_id = r.id
                LEFT JOIN relevo_vias rv ON rv.relevo_id = r.id
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();

        if (texto(req.plaza())) {
            sql.append(" AND UPPER(p.codigo) = UPPER(?)");
            params.add(req.plaza().trim());
        }
        if (texto(req.turno())) {
            sql.append(" AND UPPER(tu.codigo) = UPPER(?)");
            params.add(req.turno().trim());
        }
        if (req.codigo() != null) {
            sql.append(" AND op.codigo = ?");
            params.add(req.codigo());
        }
        if (req.relevoId() != null) {
            sql.append(" AND r.id = ?");
            params.add(req.relevoId());
        }

        agregarFechas(sql, params, req, "r.fecha");

        sql.append("""
                 GROUP BY r.id, r.fecha, r.hora, p.codigo, tu.codigo,
                          op.codigo, op.nombre_completo, r.observaciones, r.resumen
                 ORDER BY r.fecha DESC, r.hora DESC
                 LIMIT ?
                """);
        params.add(req.limiteSeguro());

        return resultado("Relevos", query(sql, params), periodo(req));
    }

    private SigoToolResult vias(SigoToolRequest req) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    r.fecha,
                    p.codigo AS plaza,
                    tu.codigo AS turno,
                    v.numero AS via,
                    v.nombre AS nombre_via,
                    rv.estado,
                    rv.detalle,
                    r.id AS relevo_id
                FROM relevo_vias rv
                JOIN relevos r ON r.id = rv.relevo_id
                JOIN vias v ON v.id = rv.via_id
                JOIN plazas p ON p.id = r.plaza_id
                JOIN turnos tu ON tu.id = r.turno_id
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();

        if (texto(req.plaza())) {
            sql.append(" AND UPPER(p.codigo) = UPPER(?)");
            params.add(req.plaza().trim());
        }
        if (texto(req.turno())) {
            sql.append(" AND UPPER(tu.codigo) = UPPER(?)");
            params.add(req.turno().trim());
        }
        if (texto(req.estado())) {
            sql.append(" AND UPPER(CAST(rv.estado AS TEXT)) = UPPER(?)");
            params.add(req.estado().trim());
        }
        if (req.relevoId() != null) {
            sql.append(" AND r.id = ?");
            params.add(req.relevoId());
        }

        agregarFechas(sql, params, req, "r.fecha");
        sql.append(" ORDER BY r.fecha DESC, v.numero LIMIT ?");
        params.add(req.limiteSeguro());

        return resultado("Reporte de vías", query(sql, params), periodo(req));
    }

    private SigoToolResult evidenciasAsistencia(SigoToolRequest req) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    ae.id AS evidencia_id,
                    ae.asistencia_id,
                    ar.fecha,
                    p.codigo AS plaza,
                    tu.codigo AS turno,
                    ae.tipo,
                    ae.url_archivo,
                    ae.subido_en
                FROM asistencia_evidencia ae
                JOIN asistencia_registro ar ON ar.id = ae.asistencia_id
                JOIN plazas p ON p.id = ar.plaza_id
                JOIN turnos tu ON tu.id = ar.turno_id
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();

        if (req.asistenciaId() != null) {
            sql.append(" AND ae.asistencia_id = ?");
            params.add(req.asistenciaId());
        }
        if (texto(req.plaza())) {
            sql.append(" AND UPPER(p.codigo) = UPPER(?)");
            params.add(req.plaza().trim());
        }
        if (texto(req.turno())) {
            sql.append(" AND UPPER(tu.codigo) = UPPER(?)");
            params.add(req.turno().trim());
        }

        agregarFechas(sql, params, req, "ar.fecha");
        sql.append(" ORDER BY ar.fecha DESC, ae.id DESC LIMIT ?");
        params.add(req.limiteSeguro());

        return resultado("Evidencias de asistencia", query(sql, params), periodo(req));
    }

    private SigoToolResult evidenciasRelevo(SigoToolRequest req) {
        StringBuilder sql = new StringBuilder("""
                SELECT * FROM (
                    SELECT
                        r.id AS relevo_id,
                        r.fecha,
                        p.codigo AS plaza,
                        'CHECKLIST' AS origen,
                        er.nombre AS elemento,
                        NULL::integer AS via,
                        rce.tipo,
                        rce.url_archivo,
                        rce.created_at
                    FROM relevo_checklist_evidencias rce
                    JOIN relevo_checklist rc ON rc.id = rce.checklist_id
                    JOIN relevos r ON r.id = rc.relevo_id
                    JOIN plazas p ON p.id = r.plaza_id
                    JOIN elementos_relevo er ON er.id = rc.elemento_id

                    UNION ALL

                    SELECT
                        r.id AS relevo_id,
                        r.fecha,
                        p.codigo AS plaza,
                        'VIA' AS origen,
                        NULL AS elemento,
                        v.numero AS via,
                        rve.tipo,
                        rve.url_archivo,
                        rve.created_at
                    FROM relevo_via_evidencias rve
                    JOIN relevo_vias rv ON rv.id = rve.relevo_via_id
                    JOIN relevos r ON r.id = rv.relevo_id
                    JOIN plazas p ON p.id = r.plaza_id
                    JOIN vias v ON v.id = rv.via_id
                ) ev
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();

        if (req.relevoId() != null) {
            sql.append(" AND ev.relevo_id = ?");
            params.add(req.relevoId());
        }
        if (texto(req.plaza())) {
            sql.append(" AND UPPER(ev.plaza) = UPPER(?)");
            params.add(req.plaza().trim());
        }

        agregarFechas(sql, params, req, "ev.fecha");
        sql.append(" ORDER BY ev.fecha DESC, ev.created_at DESC LIMIT ?");
        params.add(req.limiteSeguro());

        return resultado("Evidencias de relevos", query(sql, params), periodo(req));
    }

    private SigoToolResult resumenGeneral(SigoToolRequest req) {
        List<Map<String, Object>> filas = new ArrayList<>();

        Map<String, Object> asistencia = primeraFila(resumenAsistencia(req).filas());
        if (!asistencia.isEmpty()) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("seccion", "ASISTENCIA");
            fila.putAll(asistencia);
            filas.add(fila);
        }

        StringBuilder ausSql = new StringBuilder("""
                SELECT COUNT(*) AS total_ausencias
                FROM asistencia_ausencia aa
                JOIN asistencia_registro ar ON ar.id = aa.asistencia_id
                JOIN plazas p ON p.id = ar.plaza_id
                JOIN turnos tu ON tu.id = ar.turno_id
                WHERE 1=1
                """);
        List<Object> ausParams = new ArrayList<>();
        agregarFiltrosAsistencia(ausSql, ausParams, req, "ar", "p", "tu");
        Map<String, Object> aus = primeraFila(query(ausSql, ausParams));
        if (!aus.isEmpty()) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("seccion", "AUSENCIAS");
            fila.putAll(aus);
            filas.add(fila);
        }

        StringBuilder relSql = new StringBuilder("""
                SELECT
                    COUNT(DISTINCT r.id) AS relevos,
                    COUNT(DISTINCT CASE WHEN rc.estado <> 'OPERATIVO' THEN rc.id END) AS elementos_observados,
                    COUNT(DISTINCT CASE WHEN rv.estado <> 'OPERATIVO' THEN rv.id END) AS vias_observadas
                FROM relevos r
                JOIN plazas p ON p.id = r.plaza_id
                JOIN turnos tu ON tu.id = r.turno_id
                LEFT JOIN relevo_checklist rc ON rc.relevo_id = r.id
                LEFT JOIN relevo_vias rv ON rv.relevo_id = r.id
                WHERE 1=1
                """);
        List<Object> relParams = new ArrayList<>();

        if (texto(req.plaza())) {
            relSql.append(" AND UPPER(p.codigo) = UPPER(?)");
            relParams.add(req.plaza().trim());
        }
        if (texto(req.turno())) {
            relSql.append(" AND UPPER(tu.codigo) = UPPER(?)");
            relParams.add(req.turno().trim());
        }
        agregarFechas(relSql, relParams, req, "r.fecha");

        Map<String, Object> rel = primeraFila(query(relSql, relParams));
        if (!rel.isEmpty()) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("seccion", "RELEVOS");
            fila.putAll(rel);
            filas.add(fila);
        }

        return resultado("Resumen general de SIGO", filas, periodo(req));
    }

    private void agregarFiltrosAsistencia(
            StringBuilder sql,
            List<Object> params,
            SigoToolRequest req,
            String ar,
            String p,
            String tu
    ) {
        if (texto(req.plaza())) {
            sql.append(" AND UPPER(").append(p).append(".codigo) = UPPER(?)");
            params.add(req.plaza().trim());
        }
        if (texto(req.turno())) {
            sql.append(" AND UPPER(").append(tu).append(".codigo) = UPPER(?)");
            params.add(req.turno().trim());
        }
        if (req.asistenciaId() != null) {
            sql.append(" AND ").append(ar).append(".id = ?");
            params.add(req.asistenciaId());
        }
        agregarFechas(sql, params, req, ar + ".fecha");
    }

    private void agregarFechas(
            StringBuilder sql,
            List<Object> params,
            SigoToolRequest req,
            String columna
    ) {
        if (req.desde() != null) {
            sql.append(" AND ").append(columna).append(" >= ?");
            params.add(Date.valueOf(req.desde()));
        }
        if (req.hasta() != null) {
            sql.append(" AND ").append(columna).append(" <= ?");
            params.add(Date.valueOf(req.hasta()));
        }
    }

    private List<Map<String, Object>> query(StringBuilder sql, List<Object> params) {
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    private Map<String, Object> primeraFila(List<Map<String, Object>> filas) {
        return filas == null || filas.isEmpty() ? Map.of() : filas.getFirst();
    }

    private boolean texto(String value) {
        return value != null && !value.isBlank();
    }

    private String periodo(SigoToolRequest req) {
        if (req.desde() == null && req.hasta() == null) return null;
        if (req.desde() != null && req.hasta() != null) {
            return "Periodo: " + req.desde() + " a " + req.hasta();
        }
        if (req.desde() != null) return "Desde: " + req.desde();
        return "Hasta: " + req.hasta();
    }

    private SigoToolResult resultado(String titulo, List<Map<String, Object>> filas, String nota) {
        return new SigoToolResult(titulo, filas == null ? List.of() : filas, nota);
    }

    private SigoToolResult vacio(String titulo, String nota) {
        return new SigoToolResult(titulo, List.of(), nota);
    }
}
