package com.sigo.programacion.application.port.out;

import com.sigo.programacion.application.port.in.DistribucionUseCase;

/** Genera la matriz de distribución por plaza y rango de fechas. */
public interface DistribucionReporteExcelPort {

    byte[] generarMatriz(DistribucionUseCase.ReportePlaza reporte);
}
