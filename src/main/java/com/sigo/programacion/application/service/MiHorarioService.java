package com.sigo.programacion.application.service;

import com.sigo.programacion.application.port.in.MiHorarioUseCase;
import com.sigo.programacion.application.port.out.MiHorarioGestionPort;
import com.sigo.programacion.application.port.out.ProgramacionAccessPort;
import com.sigo.programacion.domain.ProgramacionValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class MiHorarioService implements MiHorarioUseCase {

    private final MiHorarioGestionPort miHorarioGestionPort;
    private final ProgramacionAccessPort accessPort;

    @Override
    @Transactional(readOnly = true)
    public Horario obtener(
            LocalDate desde,
            LocalDate hasta
    ) {
        if (desde == null
                || hasta == null
                || hasta.isBefore(desde)) {
            throw new ProgramacionValidationException(
                    "Rango de fechas inválido"
            );
        }

        if (ChronoUnit.DAYS.between(desde, hasta) > 31) {
            throw new ProgramacionValidationException(
                    "El rango máximo permitido es 32 días"
            );
        }

        return miHorarioGestionPort.obtener(
                accessPort.currentUserId(),
                desde,
                hasta
        );
    }
}
