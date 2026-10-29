package com.sigo.prediccion.api.dto;

import java.time.LocalDate;
import java.util.List;

public record ProyeccionFinMesResponse(
        Long plazaId,
        LocalDate fechaConsulta,
        LocalDate desde,
        LocalDate hasta,
        int turnosProyectados,
        int turnosSinProgramacion,
        int turnosYaRegistrados,
        int totalProgramados,
        int totalAusenciasEstimadas,
        int totalCoberturaEstimada,
        List<Detalle> detalle
) {
    public record Detalle(
            LocalDate fecha,
            Long turnoId,
            String turnoCodigo,
            String estado,
            Integer programados,
            Integer ausenciasEstimadas,
            Integer coberturaEstimada
    ) {}
}
