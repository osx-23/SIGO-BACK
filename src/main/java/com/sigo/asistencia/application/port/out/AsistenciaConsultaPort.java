package com.sigo.asistencia.application.port.out;

import com.sigo.asistencia.application.port.in.ConsultarAsistenciasUseCase;

import java.time.LocalDate;
import java.util.List;

public interface AsistenciaConsultaPort {

    ConsultarAsistenciasUseCase.Asistencia obtenerPorId(Long id);

    List<ConsultarAsistenciasUseCase.Asistencia> listar(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId
    );

    boolean existePlaza(Long plazaId);
}
