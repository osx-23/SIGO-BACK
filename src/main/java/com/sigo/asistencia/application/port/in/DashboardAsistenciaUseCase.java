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

    List<TopAusencia> topAusencias(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId,
            Long turnoId,
            int limite
    );

    List<AusenciaTrabajador> buscarAusenciasTrabajador(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId,
            Long turnoId,
            String consulta
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

    record TopAusencia(
            Long trabajadorId,
            Integer codigo,
            String nombre,
            Long totalAusencias
    ) {
    }

    record AusenciaTrabajador(
            Long trabajadorId,
            Integer codigo,
            String nombre,
            LocalDate fecha,
            String motivo,
            String observacion,
            String plaza,
            String turno
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
