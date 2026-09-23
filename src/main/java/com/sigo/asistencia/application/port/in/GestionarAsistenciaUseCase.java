package com.sigo.asistencia.application.port.in;

import java.time.LocalDate;
import java.util.List;

public interface GestionarAsistenciaUseCase {

    ConsultarAsistenciasUseCase.Asistencia registrar(
            Command command
    );

    ConsultarAsistenciasUseCase.Asistencia actualizar(
            Long id,
            Command command
    );

    record AusenciaCommand(
            Long trabajadorId,
            Long motivoId,
            String observacion
    ) {
    }

    record Command(
            Long plazaId,
            Long turnoId,
            Long controladorId,
            LocalDate fecha,
            Integer programados,
            Integer presentes,
            Integer apoyoSolicitado,
            String detalleApoyo,
            String notas,
            List<AusenciaCommand> ausencias,
            List<String> evidencias
    ) {
    }
}
