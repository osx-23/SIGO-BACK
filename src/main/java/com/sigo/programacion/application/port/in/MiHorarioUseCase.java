package com.sigo.programacion.application.port.in;

import java.time.LocalDate;
import java.util.List;

public interface MiHorarioUseCase {

    Horario obtener(LocalDate desde, LocalDate hasta);

    record Dia(
            LocalDate fecha,
            String estado,
            String ubicacionCodigo,
            String ubicacionNombre
    ) {
    }

    record Horario(
            Long trabajadorId,
            Integer codigo,
            String nombre,
            Long plazaId,
            String plazaCodigo,
            String lider,
            List<Dia> dias
    ) {
    }
}
