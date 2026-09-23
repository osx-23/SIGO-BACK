package com.sigo.programacion.infrastructure.persistence.adapter;

import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.programacion.application.port.in.MiHorarioUseCase;
import com.sigo.programacion.application.port.out.MiHorarioGestionPort;
import com.sigo.programacion.infrastructure.persistence.repository.AgenteControladorLiderRepository;
import com.sigo.programacion.infrastructure.persistence.repository.DistribucionPersonalRepository;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionTurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MiHorarioGestionJpaAdapter
        implements MiHorarioGestionPort {

    private final ProgramacionTurnoRepository programacionRepository;
    private final DistribucionPersonalRepository distribucionRepository;
    private final AgenteControladorLiderRepository liderRepository;
    private final TrabajadorRepository trabajadorRepository;

    @Override
    public MiHorarioUseCase.Horario obtener(
            Long trabajadorId,
            LocalDate desde,
            LocalDate hasta
    ) {
        var trabajador = trabajadorRepository
                .findHorarioResumen(trabajadorId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Usuario actual no encontrado"
                        )
                );

        Map<LocalDate, ProgramacionTurnoRepository.TurnoResumen> turnos =
                new HashMap<>();

        programacionRepository
                .findHorarioResumen(
                        trabajadorId,
                        desde,
                        hasta
                )
                .forEach(programacion ->
                        turnos.put(
                                programacion.getFecha(),
                                programacion
                        )
                );

        Map<Long, DistribucionPersonalRepository.DistribucionHorarioResumen>
                distribuciones =
                new HashMap<>();

        distribucionRepository
                .findHorarioResumen(
                        trabajadorId,
                        desde,
                        hasta
                )
                .forEach(distribucion ->
                        distribuciones.put(
                                distribucion.getProgramacionTurnoId(),
                                distribucion
                        )
                );

        List<MiHorarioUseCase.Dia> dias =
                new ArrayList<>();

        for (
                LocalDate fecha = desde;
                !fecha.isAfter(hasta);
                fecha = fecha.plusDays(1)
        ) {
            var programacion =
                    turnos.get(fecha);

            if (programacion == null) {
                dias.add(
                        new MiHorarioUseCase.Dia(
                                fecha,
                                null,
                                null,
                                null
                        )
                );
                continue;
            }

            var distribucion =
                    distribuciones.get(
                            programacion.getProgramacionId()
                    );

            dias.add(
                    new MiHorarioUseCase.Dia(
                            fecha,
                            programacion.getEstado().name(),
                            distribucion == null
                                    ? null
                                    : distribucion.getUbicacionCodigo(),
                            distribucion == null
                                    ? null
                                    : distribucion.getUbicacionNombre()
                    )
            );
        }

        String lider = liderRepository
                .findNombreLiderActivo(trabajadorId)
                .map(
                        AgenteControladorLiderRepository
                                .LiderNombreResumen::getNombreCompleto
                )
                .orElse(null);

        return new MiHorarioUseCase.Horario(
                trabajador.getId(),
                trabajador.getCodigo(),
                trabajador.getNombreCompleto(),
                trabajador.getPlazaId(),
                trabajador.getPlazaCodigo(),
                lider,
                dias
        );
    }
}
