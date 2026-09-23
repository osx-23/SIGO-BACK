package com.sigo.programacion.infrastructure.persistence.adapter;

import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.programacion.application.port.in.MiHorarioUseCase;
import com.sigo.programacion.application.port.out.MiHorarioGestionPort;
import com.sigo.programacion.infrastructure.persistence.entity.DistribucionPersonal;
import com.sigo.programacion.infrastructure.persistence.entity.ProgramacionTurno;
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
        Trabajador trabajador = trabajadorRepository
                .findById(trabajadorId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Usuario actual no encontrado"
                        )
                );

        Map<LocalDate, ProgramacionTurno> turnos =
                new HashMap<>();

        programacionRepository
                .findHorario(trabajadorId, desde, hasta)
                .forEach(programacion ->
                        turnos.put(
                                programacion.getFecha(),
                                programacion
                        )
                );

        Map<Long, DistribucionPersonal> distribuciones =
                new HashMap<>();

        distribucionRepository
                .findByTrabajadorMes(
                        trabajadorId,
                        desde,
                        hasta
                )
                .forEach(distribucion ->
                        distribuciones.put(
                                distribucion
                                        .getProgramacionTurno()
                                        .getId(),
                                distribucion
                        )
                );

        List<MiHorarioUseCase.Dia> dias =
                new ArrayList<>();

        for (LocalDate fecha = desde;
             !fecha.isAfter(hasta);
             fecha = fecha.plusDays(1)) {

            ProgramacionTurno programacion =
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

            DistribucionPersonal distribucion =
                    distribuciones.get(programacion.getId());

            dias.add(
                    new MiHorarioUseCase.Dia(
                            fecha,
                            programacion.getEstado().name(),
                            distribucion == null
                                    ? null
                                    : distribucion
                                            .getUbicacion()
                                            .getCodigo(),
                            distribucion == null
                                    ? null
                                    : distribucion
                                            .getUbicacion()
                                            .getNombre()
                    )
            );
        }

        String lider = liderRepository
                .findByAgenteIdAndActivoTrue(trabajadorId)
                .map(relacion ->
                        relacion
                                .getControlador()
                                .getNombreCompleto()
                )
                .orElse(null);

        return new MiHorarioUseCase.Horario(
                trabajador.getId(),
                trabajador.getCodigo(),
                trabajador.getNombreCompleto(),
                trabajador.getPlaza() == null
                        ? null
                        : trabajador.getPlaza().getId(),
                trabajador.getPlaza() == null
                        ? null
                        : trabajador.getPlaza().getCodigo(),
                lider,
                dias
        );
    }
}
