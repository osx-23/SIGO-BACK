package com.sigo.asistencia.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DashboardAsistenciaUseCase {

    List<Punto> diario(
            int anio,
            int mes,
            Long plazaId,
            Long turnoId
    );

    List<Punto> anual(
            int anio,
            Long plazaId,
            Long turnoId
    );

    List<Motivo> motivos(
            int anio,
            Integer mes,
            Long plazaId,
            Long turnoId
    );

    Resumen resumen(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId,
            Long turnoId
    );

    record Punto(
            Integer periodo,
            Long presentes,
            Long programados,
            BigDecimal porcentaje
    ) {
    }

    record Motivo(
            String motivo,
            Long total
    ) {
    }

    record Resumen(
            Long registros,
            Long presentes,
            Long programados,
            Long ausentes,
            BigDecimal porcentajeGeneral
    ) {
    }
}
