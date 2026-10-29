package com.sigo.programacion.infrastructure.persistence.adapter;

import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.programacion.application.port.out.TrasladoPlazaPort;
import com.sigo.programacion.domain.ProgramacionValidationException;
import com.sigo.programacion.domain.TrasladoPlaza;
import com.sigo.programacion.infrastructure.persistence.entity.EstadoTrasladoPlaza;
import com.sigo.programacion.infrastructure.persistence.entity.ProgramacionTurno;
import com.sigo.programacion.infrastructure.persistence.entity.TrasladoPlazaEntity;
import com.sigo.programacion.infrastructure.persistence.repository.DistribucionPersonalRepository;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionTurnoRepository;
import com.sigo.programacion.infrastructure.persistence.repository.TrasladoPlazaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TrasladoPlazaJpaAdapter implements TrasladoPlazaPort {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final TrasladoPlazaRepository traslados;
    private final TrabajadorRepository trabajadores;
    private final PlazaRepository plazas;
    private final ProgramacionTurnoRepository turnos;
    private final DistribucionPersonalRepository distribuciones;

    public static boolean programable(Trabajador t) {
        if (t == null || !Boolean.TRUE.equals(t.getActivo()) || t.getPuesto() == null) return false;
        String puesto = t.getPuesto().getNombre();
        return puesto != null && (puesto.startsWith("Agente de Recaud") || puesto.startsWith("Controlador"));
    }

    public static Long plazaEfectiva(Trabajador trabajador, LocalDate fecha, List<TrasladoPlazaEntity> activos) {
        return activos.stream()
                .filter(x -> x.getTrabajador().getId().equals(trabajador.getId())
                        && !fecha.isBefore(x.getFechaInicio())
                        && (x.getFechaFin() == null || !fecha.isAfter(x.getFechaFin())))
                .map(x -> x.getDestino().getId())
                .findFirst()
                .orElse(trabajador.getPlaza() == null ? null : trabajador.getPlaza().getId());
    }

    @Override
    public List<Candidato> candidatos() {
        return trabajadores.findByActivoTrueOrderByNombreCompletoAsc().stream()
                .filter(TrasladoPlazaJpaAdapter::programable)
                .filter(t -> t.getPlaza() != null)
                .map(t -> new Candidato(t.getId(), t.getCodigo(), t.getNombreCompleto(),
                        t.getPuesto().getNombre(), t.getPlaza().getId(), t.getPlaza().getCodigo()))
                .toList();
    }

    @Override
    public List<TrasladoPlaza> listar(Long plazaId, LocalDate desde, LocalDate hasta) {
        // No enviar fechas opcionales nulas a consultas JPQL con ":fecha is null".
        // PostgreSQL no puede inferir su tipo y devuelve SQLState 42P18.
        List<TrasladoPlazaEntity> registros;
        if (plazaId == null) {
            registros = traslados.consultarTodos();
        } else if (desde == null || hasta == null) {
            registros = traslados.consultarPlaza(plazaId);
        } else {
            registros = traslados.consultarPlazaPeriodo(plazaId, desde, hasta);
        }
        return registros.stream().map(this::toData).toList();
    }

    @Override
    public TrasladoPlaza crear(Long trabajadorId, Long destinoId, LocalDate inicio, LocalDate fin, Long supervisorId) {
        Trabajador trabajador = trabajadores.findById(trabajadorId)
                .orElseThrow(() -> bad("Trabajador no encontrado"));
        if (!programable(trabajador) || trabajador.getPlaza() == null) {
            throw bad("Solo se pueden trasladar agentes o controladores activos con plaza asignada");
        }
        Plaza origen = trabajador.getPlaza();
        Plaza destino = plazas.findById(destinoId)
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .orElseThrow(() -> bad("Plaza destino no disponible"));
        if (Objects.equals(origen.getId(), destino.getId())) {
            throw bad("La plaza destino debe ser distinta de la plaza actual");
        }
        List<TrasladoPlazaEntity> otros = traslados.activosDeTrabajadores(
                Set.of(trabajadorId), EstadoTrasladoPlaza.PROGRAMADO);
        for (TrasladoPlazaEntity otro : otros) {
            if ((fin == null || !fin.isBefore(otro.getFechaInicio()))
                    && (otro.getFechaFin() == null || !otro.getFechaFin().isBefore(inicio))) {
                throw bad("Ya existe un traslado que coincide con las fechas solicitadas");
            }
        }
        LocalDate hasta = fin == null ? LocalDate.of(9999, 12, 31) : fin;
        List<ProgramacionTurno> programados = turnos.findHorario(trabajadorId, inicio, hasta);
        for (ProgramacionTurno turno : programados) {
            Long guardada = turno.getPlaza().getId();
            if (!guardada.equals(origen.getId()) && !guardada.equals(destino.getId())) {
                throw bad("Existe programación en otra plaza el " + turno.getFecha() + ". Revísala antes de trasladar");
            }
        }
        // La caseta de la plaza anterior nunca se reutiliza en la nueva plaza.
        for (ProgramacionTurno turno : programados) {
            if (!turno.getPlaza().getId().equals(destino.getId())) {
                distribuciones.findByProgramacionTurnoId(turno.getId())
                        .ifPresent(distribuciones::delete);
                turno.setPlaza(destino);
                turno.setActualizadoPor(trabajadores.getReferenceById(supervisorId));
            }
        }
        distribuciones.flush();
        turnos.saveAllAndFlush(programados);

        TrasladoPlazaEntity registro = new TrasladoPlazaEntity();
        registro.setTrabajador(trabajador);
        registro.setOrigen(origen);
        registro.setDestino(destino);
        registro.setFechaInicio(inicio);
        registro.setFechaFin(fin);
        registro.setEstado(EstadoTrasladoPlaza.PROGRAMADO);
        registro.setCreadoPor(trabajadores.getReferenceById(supervisorId));
        return toData(traslados.saveAndFlush(registro));
    }

    @Override
    public void cancelar(Long id) {
        TrasladoPlazaEntity registro = traslados.findById(id)
                .orElseThrow(() -> bad("Traslado no encontrado"));
        if (registro.getEstado() != EstadoTrasladoPlaza.PROGRAMADO) {
            throw bad("El traslado ya está cancelado");
        }
        if (!registro.getFechaInicio().isAfter(LocalDate.now(LIMA))) {
            throw bad("Solo puedes cancelar traslados que todavía no han comenzado");
        }
        LocalDate hasta = registro.getFechaFin() == null
                ? LocalDate.of(9999,12,31) : registro.getFechaFin();
        List<ProgramacionTurno> programados = turnos.findHorario(
                registro.getTrabajador().getId(), registro.getFechaInicio(), hasta);
        for (ProgramacionTurno turno : programados) {
            if (turno.getPlaza().getId().equals(registro.getDestino().getId())) {
                distribuciones.findByProgramacionTurnoId(turno.getId())
                        .ifPresent(distribuciones::delete);
                turno.setPlaza(registro.getOrigen());
            }
        }
        distribuciones.flush();
        turnos.saveAllAndFlush(programados);
        registro.setEstado(EstadoTrasladoPlaza.CANCELADO);
        traslados.saveAndFlush(registro);
    }

    private TrasladoPlaza toData(TrasladoPlazaEntity t) {
        return new TrasladoPlaza(
                t.getId(), t.getTrabajador().getId(), t.getTrabajador().getCodigo(),
                t.getTrabajador().getNombreCompleto(), t.getTrabajador().getPuesto().getNombre(),
                t.getOrigen().getId(), t.getOrigen().getCodigo(),
                t.getDestino().getId(), t.getDestino().getCodigo(),
                t.getFechaInicio(), t.getFechaFin(), t.getEstado().name()
        );
    }

    private ProgramacionValidationException bad(String message) {
        return new ProgramacionValidationException(message);
    }
}
