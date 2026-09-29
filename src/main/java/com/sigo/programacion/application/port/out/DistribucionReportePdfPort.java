package com.sigo.programacion.application.port.out;

import com.sigo.programacion.application.port.in.DistribucionUseCase;

import java.time.LocalDate;

public interface DistribucionReportePdfPort {

    byte[] generar(
            DistribucionUseCase.ReporteTrabajador reporte,
            LocalDate desde,
            LocalDate hasta
    );

    byte[] generarMatriz(
            DistribucionUseCase.ReportePlaza reporte
    );
}
