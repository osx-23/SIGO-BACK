package com.sigo.prediccion.infrastructure;

import com.sigo.asistencia.infrastructure.persistence.repository.AsistenciaRegistroRepository;
import com.sigo.personal.infrastructure.persistence.repository.TurnoRepository;
import com.sigo.prediccion.application.port.out.ProyeccionDatosPort;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionTurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProyeccionDatosJpaAdapter implements ProyeccionDatosPort {
    private final ProgramacionTurnoRepository programacionRepository;
    private final AsistenciaRegistroRepository asistenciaRepository;
    private final TurnoRepository turnoRepository;

    @Override
    public List<TurnoOperativo> listarTurnos() {
        return turnoRepository.findAll().stream()
                .map(t -> new TurnoOperativo(t.getId(), t.getCodigo()))
                .toList();
    }

    @Override
    public List<TurnoProgramado> programacion(Long plazaId, LocalDate desde, LocalDate hasta) {
        return programacionRepository.findMesResumen(plazaId, desde, hasta).stream()
                .filter(f -> f.getEstado() != null && f.getEstado().esOperativo())
                .map(f -> new TurnoProgramado(f.getFecha(), f.getTrabajadorId(), f.getEstado().name()))
                .toList();
    }

    @Override
    public List<AsistenciaRegistrada> asistencias(Long plazaId, LocalDate desde, LocalDate hasta) {
        return asistenciaRepository.findByFechaBetweenAndPlazaIdOrderByFechaDescIdDesc(
                        desde, hasta, plazaId).stream()
                .filter(a -> a.getTurno() != null)
                .map(a -> new AsistenciaRegistrada(a.getFecha(), a.getTurno().getId()))
                .toList();
    }
}
