package com.sigo.asistencia.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ConsultarAsistenciasUseCase {

    Asistencia obtenerPorId(Long id);

    List<Asistencia> listar(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId
    );

    record Ausencia(
            Long id,
            Long trabajadorId,
            Integer codigoTrabajador,
            String nombreTrabajador,
            Long motivoId,
            String motivo,
            String observacion
    ) {
    }

    record Evidencia(
            Long id,
            String urlArchivo,
            String tipo
    ) {
    }

    record Asistencia(
            Long id,
            Long plazaId,
            String plaza,
            Long turnoId,
            String turno,
            Long controladorId,
            String controlador,
            LocalDate fecha,
            Integer programados,
            Integer presentes,
            Integer ausentes,
            Integer apoyoSolicitado,
            String detalleApoyo,
            BigDecimal porcentaje,
            String notas,
            List<Ausencia> ausencias,
            List<Evidencia> evidencias
    ) {
    }
}
