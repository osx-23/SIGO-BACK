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

    List<TopAusenciaData> topAusencias(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId,
            Long turnoId,
            int limite
    );

    List<AusenciaTrabajadorData> buscarAusenciasTrabajador(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId,
            Long turnoId,
            String consulta
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

    record TopAusenciaData(
            Long trabajadorId,
            Integer codigo,
            String nombre,
            Long totalAusencias
    ) {
    }

    record AusenciaTrabajadorData(
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

    record ResumenData(
            Long registros,
            Long presentes,
            Long programados,
            Long ausentes
    ) {
    }
}
