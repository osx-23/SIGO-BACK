package com.sigo.programacion.infrastructure.persistence.adapter;

import com.sigo.programacion.application.port.in.MiHorarioUseCase;
import com.sigo.programacion.application.port.out.MiHorarioGestionPort;
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

    @Override
    public MiHorarioUseCase.Horario obtener(
            Long trabajadorId,
            LocalDate desde,
            LocalDate hasta
    ) {
        List<ProgramacionTurnoRepository.HorarioCompletoResumen> filas =
                programacionRepository.findHorarioCompleto(
                        trabajadorId,
                        desde,
                        hasta
                );

        if (filas.isEmpty()) {
            throw new IllegalStateException(
                    "Usuario actual no encontrado"
            );
        }

        var trabajador = filas.getFirst();

        Map<LocalDate, ProgramacionTurnoRepository.HorarioCompletoResumen>
                porFecha =
                new HashMap<>();

        for (var fila : filas) {
            if (fila.getFecha() != null) {
                porFecha.put(
                        fila.getFecha(),
                        fila
                );
            }
        }

        List<MiHorarioUseCase.Dia> dias =
                new ArrayList<>();

        for (
                LocalDate fecha = desde;
                !fecha.isAfter(hasta);
                fecha = fecha.plusDays(1)
        ) {
            var fila =
                    porFecha.get(fecha);

            dias.add(
                    new MiHorarioUseCase.Dia(
                            fecha,
                            fila == null || fila.getEstado() == null
                                    ? null
                                    : fila.getEstado().name(),
                            fila == null
                                    ? null
                                    : fila.getUbicacionCodigo(),
                            fila == null
                                    ? null
                                    : fila.getUbicacionNombre()
                    )
            );
        }

        return new MiHorarioUseCase.Horario(
                trabajador.getTrabajadorId(),
                trabajador.getCodigoTrabajador(),
                trabajador.getNombreTrabajador(),
                trabajador.getPlazaId(),
                trabajador.getPlazaCodigo(),
                trabajador.getLiderNombre(),
                dias
        );
    }
}
