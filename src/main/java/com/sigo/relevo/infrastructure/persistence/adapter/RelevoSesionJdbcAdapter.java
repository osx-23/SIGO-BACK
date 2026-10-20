package com.sigo.relevo.infrastructure.persistence.adapter;

import com.sigo.relevo.application.port.in.RelevoSesionUseCase;
import com.sigo.relevo.application.port.out.RelevoSesionPort;
import com.sigo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RelevoSesionJdbcAdapter implements RelevoSesionPort {
    private final JdbcTemplate jdbc;

    private static final String SELECT = """
            SELECT id, operador_id, plaza_id, turno_id, estado,
                   iniciado_en, finalizado_en, relevo_id, borrador_json
            FROM relevo_sesiones
            """;

    private final RowMapper<RelevoSesionUseCase.Sesion> mapper = (rs, i) ->
            new RelevoSesionUseCase.Sesion(
                    rs.getLong("id"),
                    rs.getLong("operador_id"),
                    rs.getLong("plaza_id"),
                    rs.getLong("turno_id"),
                    rs.getString("estado"),
                    rs.getObject("iniciado_en", java.time.OffsetDateTime.class),
                    rs.getObject("finalizado_en", java.time.OffsetDateTime.class),
                    rs.getObject("relevo_id") == null ? null : rs.getLong("relevo_id"),
                    rs.getString("borrador_json")
            );

    @Override
    public Optional<RelevoSesionUseCase.Sesion> enProceso(Long operadorId) {
        return jdbc.query(SELECT + """
                WHERE operador_id = ? AND estado = 'EN_PROCESO'
                ORDER BY iniciado_en DESC LIMIT 1
                """, mapper, operadorId).stream().findFirst();
    }

    @Override
    public Optional<RelevoSesionUseCase.Sesion> obtener(Long id) {
        return jdbc.query(SELECT + " WHERE id = ?", mapper, id).stream().findFirst();
    }

    @Override
    public RelevoSesionUseCase.Sesion crear(Long operadorId, Long plazaId, Long turnoId) {
        Long id = jdbc.queryForObject("""
                INSERT INTO relevo_sesiones (operador_id, plaza_id, turno_id, estado, iniciado_en)
                VALUES (?, ?, ?, 'EN_PROCESO', CURRENT_TIMESTAMP) RETURNING id
                """, Long.class, operadorId, plazaId, turnoId);
        return requerir(id);
    }

    @Override
    public RelevoSesionUseCase.Sesion guardarBorrador(Long id, String borrador) {
        actualizar(id, "UPDATE relevo_sesiones SET borrador_json = ? WHERE id = ? AND estado = 'EN_PROCESO'",
                borrador);
        return requerir(id);
    }

    @Override
    public RelevoSesionUseCase.Sesion finalizar(Long id, Long relevoId) {
        actualizar(id, """
                UPDATE relevo_sesiones
                SET estado = 'FINALIZADO', finalizado_en = CURRENT_TIMESTAMP, relevo_id = ?
                WHERE id = ? AND estado = 'EN_PROCESO'
                """, relevoId);
        return requerir(id);
    }

    @Override
    public RelevoSesionUseCase.Sesion cancelar(Long id) {
        actualizar(id, """
                UPDATE relevo_sesiones
                SET estado = 'ANULADO', finalizado_en = CURRENT_TIMESTAMP
                WHERE id = ? AND estado = 'EN_PROCESO'
                """, null);
        return requerir(id);
    }

    private void actualizar(Long id, String sql, Object arg) {
        int actualizados = arg == null && sql.contains("SET estado = 'ANULADO'")
                ? jdbc.update(sql, id)
                : jdbc.update(sql, arg, id);
        if (actualizados != 1) {
            throw new BusinessException("La sesión de relevo ya no se encuentra en proceso");
        }
    }

    private RelevoSesionUseCase.Sesion requerir(Long id) {
        return obtener(id).orElseThrow(() ->
                new BusinessException("No se encontró la sesión de relevo"));
    }
}
