package com.sigo.programacion.application.port.out;

import com.sigo.programacion.application.port.in.MiHorarioUseCase;

import java.time.LocalDate;

public interface MiHorarioGestionPort {

    MiHorarioUseCase.Horario obtener(
            Long trabajadorId,
            LocalDate desde,
            LocalDate hasta
    );
}
