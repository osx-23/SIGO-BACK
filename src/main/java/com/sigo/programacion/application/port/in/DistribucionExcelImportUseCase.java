package com.sigo.programacion.application.port.in;

import java.time.LocalDate;
import java.util.List;

/** Vista previa de distribución desde Excel; no modifica los datos operativos. */
public interface DistribucionExcelImportUseCase {
    record Cambio(Long programacionTurnoId, Long ubicacionId,
                  Integer codigoTrabajador, String trabajador,
                  LocalDate fecha, String caseta) {}
    record VistaPrevia(boolean valido, Long plazaId, LocalDate desde, LocalDate hasta,
                       int celdasLeidas, int yaAsignadas,
                       List<Cambio> cambios, List<String> errores, List<String> omitidos) {}
    record RegistroJson(Integer codigoTrabajador, LocalDate fecha, String caseta,
                        String turnoExcel) {}
    record ExtraccionJson(Long plazaId, String plazaCodigo, LocalDate desde,
                          LocalDate hasta, List<RegistroJson> registros) {}

    VistaPrevia previsualizar(Long plazaId, byte[] archivo, Integer anio, Integer mes);
    ExtraccionJson extraerJson(Long plazaId, byte[] archivo, int anio, int mes);
    VistaPrevia previsualizarJson(ExtraccionJson datos);
}
