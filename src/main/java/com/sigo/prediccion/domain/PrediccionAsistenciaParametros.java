package com.sigo.prediccion.domain;

import java.time.LocalDate;

public record PrediccionAsistenciaParametros(
        Long plazaId,
        Long turnoId,
        LocalDate fecha,
        Integer programados
) {
}
