package com.sigo.programacion.application.port.out;

import com.sigo.programacion.application.port.in.GuardarTurnosUseCase;
import com.sigo.programacion.application.port.in.ListarTurnosUseCase;

import java.time.LocalDate;
import java.util.List;

public interface TurnoGestionPort {

    List<ListarTurnosUseCase.Turno> listar(
            Long plazaId,
            LocalDate desde,
            LocalDate hasta
    );

    List<ListarTurnosUseCase.Turno> guardar(
            Long plazaId,
            List<GuardarTurnosUseCase.Item> programaciones,
            Long supervisorId
    );
}
