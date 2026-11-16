package com.sigo.asistencia.infrastructure.persistence.adapter;

import com.sigo.asistencia.application.port.in.PermutaUseCase;
import com.sigo.shared.exception.ResourceNotFoundException;
import com.sigo.shared.exception.ConflictException;
import com.sigo.asistencia.application.port.in.PermutaUseCase.*;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.personal.infrastructure.persistence.repository.TurnoRepository;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PermutaJpaAdapter implements PermutaUseCase {
    private final JdbcTemplate jdbc;
    private final TrabajadorRepository trabajadores;
    private final TurnoRepository turnos;
    private final PlazaRepository plazas;

    /**
     * Solo verifica el código y puesto del agente. La programación
     * mensual no interviene: plazas, fechas y turnos se ingresan manualmente.
     */
    private Trabajador obtenerAgente(int codigo) {
        Trabajador trabajador = trabajadores.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un trabajador con código " + codigo));
        String puesto = trabajador.getPuesto() == null ? "" :
                trabajador.getPuesto().getNombre();
        if (!Boolean.TRUE.equals(trabajador.getActivo()) ||
                !puesto.toLowerCase(Locale.ROOT).contains("recaud")) {
            throw new IllegalArgumentException(
                    "El código " + codigo + " no pertenece a un agente de recaudación activo");
        }
        return trabajador;
    }

    @Transactional(readOnly = true)
    public AgenteIdentificado agente(int codigo) {
        Trabajador t = obtenerAgente(codigo);
        return new AgenteIdentificado(t.getCodigo(), t.getNombreCompleto());
    }

    private void validarTurno(Long turnoId) {
        var turno = turnos.findById(turnoId)
                .orElseThrow(() -> new IllegalArgumentException("Turno no encontrado"));
        if (!turno.getCodigo().trim().matches("[ABC]")) {
            throw new IllegalArgumentException("Solo se aceptan turnos A, B o C");
        }
    }

    private void validarPlaza(Long plazaId) {
        var plaza = plazas.findById(plazaId)
                .orElseThrow(() -> new IllegalArgumentException("Plaza no encontrada"));
        if (!Boolean.TRUE.equals(plaza.getActivo())) {
            throw new IllegalArgumentException("La plaza seleccionada está inactiva");
        }
    }

    @Transactional
    public void registrarDesdeAsistencia(Long asistenciaId, Long plazaId, Long turnoId,
                                         LocalDate fecha, List<PermutaCommand> solicitudes) {
        if (solicitudes == null || solicitudes.isEmpty()) return;
        /*
         * Protege el chequeo de duplicados frente a dos controladores registrando
         * simultáneamente. El bloqueo es por agente/fecha y dura la transacción.
         * Orden fijo para evitar interbloqueos al guardar varias permutas.
         */
        SortedSet<String> claves = new TreeSet<>();
        for (PermutaCommand s : solicitudes) {
            claves.add("SIGO:PERMUTA:" + s.codigoSolicitante() + ":" + s.fechaSolicitante());
            claves.add("SIGO:PERMUTA:" + s.codigoAceptante() + ":" + s.fechaAceptante());
        }
        for (String clave : claves) {
            jdbc.query(
                    "SELECT pg_advisory_xact_lock(hashtextextended(CAST(? AS text), 0))",
                    (java.sql.ResultSet rs) -> { rs.next(); return null; }, clave);
        }
        Set<String> participantes = new HashSet<>();
        for (PermutaCommand s : solicitudes) {
            if (s.codigoSolicitante() == null || s.codigoAceptante() == null ||
                    s.fechaSolicitante() == null || s.fechaAceptante() == null ||
                    s.plazaSolicitanteId() == null || s.plazaAceptanteId() == null ||
                    s.turnoSolicitanteId() == null || s.turnoAceptanteId() == null) {
                throw new IllegalArgumentException("Completa todos los campos de la permuta");
            }
            Trabajador sol = obtenerAgente(s.codigoSolicitante());
            Trabajador ace = obtenerAgente(s.codigoAceptante());
            if (sol.getId().equals(ace.getId())) {
                throw new IllegalArgumentException(
                        "Solicitante y aceptante deben ser personas distintas");
            }

            validarTurno(s.turnoSolicitanteId());
            validarTurno(s.turnoAceptanteId());
            validarPlaza(s.plazaSolicitanteId());
            validarPlaza(s.plazaAceptanteId());

            if (Objects.equals(s.plazaSolicitanteId(), s.plazaAceptanteId()) &&
                    Objects.equals(s.turnoSolicitanteId(), s.turnoAceptanteId()) &&
                    s.fechaSolicitante().equals(s.fechaAceptante())) {
                throw new IllegalArgumentException(
                        "Las dos jornadas son idénticas; no hay un intercambio real");
            }

            boolean correspondeTurno =
                    (Objects.equals(s.plazaSolicitanteId(), plazaId) &&
                            Objects.equals(s.turnoSolicitanteId(), turnoId) &&
                            s.fechaSolicitante().equals(fecha)) ||
                    (Objects.equals(s.plazaAceptanteId(), plazaId) &&
                            Objects.equals(s.turnoAceptanteId(), turnoId) &&
                            s.fechaAceptante().equals(fecha));
            if (!correspondeTurno) {
                throw new IllegalArgumentException(
                        "Al menos uno de los turnos originales debe coincidir con esta asistencia");
            }

            for (var lado : List.of(
                    new AbstractMap.SimpleEntry<>(sol.getCodigo(), s.fechaSolicitante()),
                    new AbstractMap.SimpleEntry<>(ace.getCodigo(), s.fechaAceptante()))) {
                String clave = lado.getKey() + ":" + lado.getValue();
                if (!participantes.add(clave)) {
                    throw new ConflictException(
                            "El mismo agente aparece en más de una permuta para esa fecha");
                }
                Integer existentes = jdbc.queryForObject("""
                    SELECT count(*) FROM asistencia_permuta
                    WHERE (codigo_solicitante = ? AND fecha_solicitante = ?)
                       OR (codigo_aceptante = ? AND fecha_aceptante = ?)
                    """, Integer.class, lado.getKey(), Date.valueOf(lado.getValue()),
                        lado.getKey(), Date.valueOf(lado.getValue()));
                if (existentes != null && existentes > 0) {
                    throw new ConflictException(
                            "Ya existe una permuta del agente " + lado.getKey() +
                                    " el " + lado.getValue());
                }
            }

            jdbc.update("""
                INSERT INTO asistencia_permuta
                    (asistencia_id, solicitante_id, codigo_solicitante, plaza_solicitante_id,
                     turno_solicitante_id, fecha_solicitante, aceptante_id, codigo_aceptante,
                     plaza_aceptante_id, turno_aceptante_id, fecha_aceptante)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                    asistenciaId, sol.getId(), sol.getCodigo(),
                    s.plazaSolicitanteId(), s.turnoSolicitanteId(),
                    Date.valueOf(s.fechaSolicitante()),
                    ace.getId(), ace.getCodigo(), s.plazaAceptanteId(),
                    s.turnoAceptanteId(), Date.valueOf(s.fechaAceptante()));
        }
    }

    private List<PermutaDetalle> consultar(LocalDate inicio, LocalDate fin) {
        LocalDate desde = inicio == null ? LocalDate.of(2000, 1, 1) : inicio;
        LocalDate hasta = fin == null ? LocalDate.of(2100, 12, 31) : fin;
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException( "La fecha inicial supera la final");
        }
        return jdbc.query("""
            SELECT a.id, ps.id AS ps_id, ps.codigo AS ps, a.codigo_solicitante,
                   s.nombre_completo AS ns, ts.id AS ts_id, ts.codigo AS ts,
                   a.fecha_solicitante, pa.id AS pa_id, pa.codigo AS pa,
                   a.codigo_aceptante, ac.nombre_completo AS na, ta.id AS ta_id,
                   ta.codigo AS ta, a.fecha_aceptante
            FROM asistencia_permuta a
            JOIN trabajadores s ON s.id = a.solicitante_id
            JOIN trabajadores ac ON ac.id = a.aceptante_id
            JOIN plazas ps ON ps.id = a.plaza_solicitante_id
            JOIN plazas pa ON pa.id = a.plaza_aceptante_id
            JOIN turnos ts ON ts.id = a.turno_solicitante_id
            JOIN turnos ta ON ta.id = a.turno_aceptante_id
            WHERE (a.fecha_solicitante BETWEEN ? AND ?
                OR a.fecha_aceptante BETWEEN ? AND ?)
            ORDER BY a.fecha_solicitante DESC, a.id DESC
            """, (rs, row) -> new PermutaDetalle(
                rs.getLong("id"), rs.getLong("ps_id"), rs.getString("ps"),
                rs.getInt("codigo_solicitante"), rs.getString("ns"),
                rs.getLong("ts_id"), rs.getString("ts").trim(),
                rs.getDate("fecha_solicitante").toLocalDate(),
                rs.getLong("pa_id"), rs.getString("pa"),
                rs.getInt("codigo_aceptante"), rs.getString("na"),
                rs.getLong("ta_id"), rs.getString("ta").trim(),
                rs.getDate("fecha_aceptante").toLocalDate()),
                Date.valueOf(desde), Date.valueOf(hasta), Date.valueOf(desde), Date.valueOf(hasta));
    }

    @Transactional(readOnly = true)
    public List<PermutaDetalle> listar(LocalDate inicio, LocalDate fin,
                                       Long plazaId, Integer codigo, Long turnoId) {
        return consultar(inicio, fin).stream()
                .filter(p -> plazaId == null ||
                        Objects.equals(plazaId, p.plazaSolicitanteId()) ||
                        Objects.equals(plazaId, p.plazaAceptanteId()))
                .filter(p -> codigo == null ||
                        Objects.equals(codigo, p.codigoSolicitante()) ||
                        Objects.equals(codigo, p.codigoAceptante()))
                .filter(p -> turnoId == null ||
                        Objects.equals(turnoId, p.turnoSolicitanteId()) ||
                        Objects.equals(turnoId, p.turnoAceptanteId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermutaDetalle> delTurno(Long plazaId, Long turnoId, LocalDate fecha) {
        return consultar(fecha, fecha).stream()
                .filter(p -> (Objects.equals(plazaId, p.plazaSolicitanteId()) &&
                        Objects.equals(turnoId, p.turnoSolicitanteId()) &&
                        fecha.equals(p.fechaSolicitante())) ||
                        (Objects.equals(plazaId, p.plazaAceptanteId()) &&
                        Objects.equals(turnoId, p.turnoAceptanteId()) &&
                        fecha.equals(p.fechaAceptante())))
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardPermutas dashboard(YearMonth mes) {
        var registros = consultar(mes.atDay(1), mes.atEndOfMonth());
        Map<String, Long> plazas = new LinkedHashMap<>();
        jdbc.queryForList("SELECT codigo FROM plazas WHERE activo = true ORDER BY id", String.class)
                .forEach(codigo -> plazas.put(codigo, 0L));
        Map<Integer, String> nombres = new HashMap<>();
        Map<Integer, Long> agentes = new HashMap<>();
        for (PermutaDetalle p : registros) {
            boolean mesSolicitante = YearMonth.from(p.fechaSolicitante()).equals(mes);
            boolean mesAceptante = YearMonth.from(p.fechaAceptante()).equals(mes);
            if (mesSolicitante) {
                plazas.merge(p.plazaSolicitante(), 1L, Long::sum);
                agentes.merge(p.codigoSolicitante(), 1L, Long::sum);
                nombres.put(p.codigoSolicitante(), p.nombreSolicitante());
            }
            if (mesAceptante) {
                if (!mesSolicitante || !p.plazaAceptante().equals(p.plazaSolicitante())) {
                    plazas.merge(p.plazaAceptante(), 1L, Long::sum);
                }
                agentes.merge(p.codigoAceptante(), 1L, Long::sum);
                nombres.put(p.codigoAceptante(), p.nombreAceptante());
            }
        }
        var porPlaza = plazas.entrySet().stream()
                .map(e -> new TotalPlaza(e.getKey(), e.getValue())).toList();
        var top = agentes.entrySet().stream()
                .map(e -> new TopAgente(e.getKey(), nombres.get(e.getKey()), e.getValue()))
                .sorted(Comparator.comparingLong(TopAgente::total).reversed()
                        .thenComparing(TopAgente::codigo))
                .limit(10).toList();
        return new DashboardPermutas(registros.size(), porPlaza, top);
    }
}
