package com.sigo.asistencia.infrastructure.persistence.adapter;

import com.sigo.asistencia.application.port.in.PermutaUseCase;
import com.sigo.shared.exception.ResourceNotFoundException;
import com.sigo.shared.exception.ConflictException;
import com.sigo.asistencia.application.port.in.PermutaUseCase.*;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.personal.infrastructure.persistence.repository.TurnoRepository;
import com.sigo.programacion.infrastructure.persistence.entity.ProgramacionTurno;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionTurnoRepository;
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
    private final ProgramacionTurnoRepository programaciones;
    private final TurnoRepository turnos;

    public AgenteProgramado agente(int codigo, LocalDate fecha) {
        Trabajador t = trabajadores.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un trabajador con código " + codigo));
        String puesto = t.getPuesto() == null ? "" : t.getPuesto().getNombre();
        if (!Boolean.TRUE.equals(t.getActivo()) ||
                !puesto.toLowerCase(Locale.ROOT).contains("recaud")) {
            throw new IllegalArgumentException(
                    "El código " + codigo + " no corresponde a un agente de recaudación activo");
        }
        ProgramacionTurno p = programaciones.findByTrabajadorIdAndFecha(t.getId(), fecha)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay programación para el agente " + codigo + " el " + fecha));
        if (p.getEstado() == null || !p.getEstado().esOperativo()) {
            throw new IllegalArgumentException(
                    "El agente " + codigo + " no tiene turno A, B o C el " + fecha);
        }
        var turno = turnos.findAll().stream()
                .filter(x -> x.getCodigo().trim().equalsIgnoreCase(p.getEstado().name()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el turno " + p.getEstado().name() + " en el catálogo"));
        return new AgenteProgramado(
                t.getCodigo(), t.getNombreCompleto(), p.getPlaza().getId(),
                p.getPlaza().getCodigo(), turno.getId(), turno.getCodigo().trim(), fecha
        );
    }


    /**
     * Devuelve únicamente agentes de recaudación activos, realmente programados
     * en el turno indicado y que aún no participan en una permuta ese día.
     * Un turno con varios candidatos NUNCA autoriza a elegir uno arbitrariamente.
     */
    @Transactional(readOnly = true)
    public List<AgenteProgramado> candidatos(Long plazaId, Long turnoId, LocalDate fecha) {
        var turno = turnos.findById(turnoId).orElseThrow(() ->
                new ResourceNotFoundException("Turno no encontrado: " + turnoId));
        String codigoTurno = turno.getCodigo().trim();
        List<Integer> ocupados = jdbc.queryForList("""
            SELECT codigo_solicitante AS codigo
              FROM asistencia_permuta WHERE fecha_solicitante = ?
            UNION
            SELECT codigo_aceptante AS codigo
              FROM asistencia_permuta WHERE fecha_aceptante = ?
            """, Integer.class, Date.valueOf(fecha), Date.valueOf(fecha));
        Set<Integer> reservados = new HashSet<>(ocupados);
        return programaciones.findMes(plazaId, fecha, fecha).stream()
                .filter(p -> p.getEstado() != null
                        && p.getEstado().esOperativo()
                        && p.getEstado().name().equalsIgnoreCase(codigoTurno))
                .filter(p -> p.getTrabajador() != null
                        && Boolean.TRUE.equals(p.getTrabajador().getActivo())
                        && p.getTrabajador().getPuesto() != null
                        && p.getTrabajador().getPuesto().getNombre()
                              .toLowerCase(Locale.ROOT).contains("recaud"))
                .filter(p -> !reservados.contains(p.getTrabajador().getCodigo()))
                .map(p -> new AgenteProgramado(
                        p.getTrabajador().getCodigo(),
                        p.getTrabajador().getNombreCompleto(),
                        p.getPlaza().getId(), p.getPlaza().getCodigo(),
                        turno.getId(), codigoTurno, fecha))
                .sorted(Comparator.comparing(AgenteProgramado::nombre))
                .toList();
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
            AgenteProgramado sol = agente(s.codigoSolicitante(), s.fechaSolicitante());
            AgenteProgramado ace = agente(s.codigoAceptante(), s.fechaAceptante());
            if (sol.codigo() == ace.codigo()) {
                throw new IllegalArgumentException(
                        "El solicitante y el aceptante deben ser personas distintas");
            }
            if (!Objects.equals(sol.turnoId(), s.turnoSolicitanteId()) ||
                    !Objects.equals(ace.turnoId(), s.turnoAceptanteId())) {
                throw new IllegalArgumentException(
                        "La programación de uno de los agentes cambió; vuelve a consultar su turno");
            }
            if (Objects.equals(sol.plazaId(), ace.plazaId()) &&
                    Objects.equals(sol.turnoId(), ace.turnoId()) &&
                    Objects.equals(sol.fecha(), ace.fecha())) {
                throw new IllegalArgumentException(
                        "Ambos agentes ya tienen el mismo turno; no existe cambio efectivo");
            }
            boolean correspondeTurno = (Objects.equals(sol.plazaId(), plazaId) &&
                    Objects.equals(sol.turnoId(), turnoId) && sol.fecha().equals(fecha))
                    || (Objects.equals(ace.plazaId(), plazaId) &&
                    Objects.equals(ace.turnoId(), turnoId) && ace.fecha().equals(fecha));
            if (!correspondeTurno) {
                throw new IllegalArgumentException(
                        "Al menos uno de los turnos de la permuta debe coincidir con la asistencia registrada");
            }
            for (AgenteProgramado a : List.of(sol, ace)) {
                String clave = a.codigo() + ":" + a.fecha();
                if (!participantes.add(clave)) {
                    throw new ConflictException(
                            "Un agente no puede aparecer en más de una permuta para la misma fecha");
                }
                Integer existentes = jdbc.queryForObject("""
                    SELECT count(*) FROM asistencia_permuta
                    WHERE (codigo_solicitante = ? AND fecha_solicitante = ?)
                       OR (codigo_aceptante = ? AND fecha_aceptante = ?)
                    """, Integer.class, a.codigo(), Date.valueOf(a.fecha()),
                        a.codigo(), Date.valueOf(a.fecha()));
                if (existentes != null && existentes > 0) {
                    throw new ConflictException(
                            "Ya existe una permuta del agente " + a.codigo() + " el " + a.fecha());
                }
            }
            jdbc.update("""
                INSERT INTO asistencia_permuta
                    (asistencia_id, solicitante_id, codigo_solicitante, plaza_solicitante_id,
                     turno_solicitante_id, fecha_solicitante, aceptante_id, codigo_aceptante,
                     plaza_aceptante_id, turno_aceptante_id, fecha_aceptante)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                asistenciaId,
                trabajadores.findByCodigo(sol.codigo()).orElseThrow().getId(),
                sol.codigo(), sol.plazaId(), sol.turnoId(), Date.valueOf(sol.fecha()),
                trabajadores.findByCodigo(ace.codigo()).orElseThrow().getId(),
                ace.codigo(), ace.plazaId(), ace.turnoId(), Date.valueOf(ace.fecha()));
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
