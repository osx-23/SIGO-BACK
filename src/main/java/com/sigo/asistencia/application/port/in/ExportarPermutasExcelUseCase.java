package com.sigo.asistencia.application.port.in;

import java.time.LocalDate;

/** Exporta el consolidado anual y los registros del período filtrado. */
public interface ExportarPermutasExcelUseCase {
    byte[] exportar(int anio, LocalDate inicio, LocalDate fin,
                    Long plazaId, Long turnoId, Integer codigo);
}
