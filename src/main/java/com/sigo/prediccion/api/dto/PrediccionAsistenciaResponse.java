package com.sigo.prediccion.api.dto;

import java.time.LocalDate;

public record PrediccionAsistenciaResponse(
        Long plazaId,
        Long turnoId,
        LocalDate fecha,
        Integer programados,
        Integer ausenciasEstimadas,
        Integer coberturaEstimada,
        String modelo,
        String version,
        String origenDatos,
        String advertencia
) {
}
