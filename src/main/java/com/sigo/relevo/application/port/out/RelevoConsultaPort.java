package com.sigo.relevo.application.port.out;

import com.sigo.relevo.application.port.in.ConsultarRelevosUseCase;

import java.time.LocalDate;
import java.util.List;

public interface RelevoConsultaPort {

    List<ConsultarRelevosUseCase.Elemento> listarElementos();

    ConsultarRelevosUseCase.Relevo obtener(Long id);

    List<ConsultarRelevosUseCase.Relevo> listar(
            LocalDate inicio,
            LocalDate fin
    );
}
