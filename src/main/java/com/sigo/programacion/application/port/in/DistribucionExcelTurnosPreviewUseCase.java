package com.sigo.programacion.application.port.in;

import java.time.LocalDate;
import java.util.List;

/** Lectura mensual de turnos y casetas desde Excel, sin escritura. */
public interface DistribucionExcelTurnosPreviewUseCase {
    record Propuesta(int codigoTrabajador, String trabajador, LocalDate fecha,
                     String valorExcel, String turnoExcel, String turnoSigo,
                     String casetaSigo, String accion, String detalle) {}
    record VistaPrevia(LocalDate desde, LocalDate hasta, int leidas,
                       int crear, int actualizar, int coinciden, int revisar,
                       List<Propuesta> propuestas, List<String> advertencias) {}
    VistaPrevia previsualizar(Long plazaId, byte[] archivo, Integer anio, Integer mes);
}
