package com.sigo.asistencia.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DashboardAsistenciaQueryPort {

    List<PuntoData> diario(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId,
            Long turnoId
    );

    List<PuntoData> anual(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId,
            Long turnoId
    );

    List<MotivoData> motivos(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId,
            Long turnoId
    );

    ResumenData resumen(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId,
            Long turnoId
    );

    record PuntoData(
            Integer periodo,
            Long presentes,
            Long programados,
            BigDecimal porcentaje
    ) {
    }

    record MotivoData(
            String motivo,
            Long total
    ) {
    }

    record ResumenData(
            Long registros,
            Long presentes,
            Long programados,
            Long ausentes
    ) {
    }
}
