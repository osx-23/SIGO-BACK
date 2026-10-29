package com.sigo.programacion.domain;

import java.time.LocalDate;

/** Contrato de dominio sin dependencias de HTTP ni JPA. */
public record TrasladoPlaza(
        Long id, Long trabajadorId, Integer codigo, String trabajador,
        String puesto, Long plazaOrigenId, String plazaOrigenCodigo,
        Long plazaDestinoId, String plazaDestinoCodigo,
        LocalDate fechaInicio, LocalDate fechaFin, String estado
) {}
