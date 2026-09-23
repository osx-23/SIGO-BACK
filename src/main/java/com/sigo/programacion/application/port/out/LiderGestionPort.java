package com.sigo.programacion.application.port.out;

import com.sigo.programacion.application.port.in.ListarLideresUseCase;

import java.time.LocalDate;
import java.util.List;

public interface LiderGestionPort {

    List<ListarLideresUseCase.Lider> listar(Long plazaId);

    ListarLideresUseCase.Lider asignar(
            Long agenteId,
            Long controladorId,
            Long plazaId,
            LocalDate fechaInicio,
            Long asignadoPorId
    );
}
