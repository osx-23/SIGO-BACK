package com.sigo.prediccion.domain;

import java.time.LocalDate;

public record PrediccionAsistenciaResultado(
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
