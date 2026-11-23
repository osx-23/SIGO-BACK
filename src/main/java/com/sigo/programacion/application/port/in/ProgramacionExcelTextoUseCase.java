package com.sigo.programacion.application.port.in;

import java.time.LocalDate;
import java.util.List;

/** Vista previa de los estados de turnos del Excel, sin persistencia. */
public interface ProgramacionExcelTextoUseCase {
    record Cambio(Long trabajadorId, Integer codigoTrabajador, String nombre,
                  LocalDate fecha, String anterior, String nuevo) {}
    record PersonalPendiente(Integer codigo, String nombre, int celdas, boolean registrable,
                            String motivo) {}
    record VistaPrevia(int leidos, int coinciden, List<Cambio> cambios,
                       List<String> errores, List<String> omitidos,
                       List<PersonalPendiente> personalPendiente) {}
    VistaPrevia previsualizar(Long plazaId, int anio, int mes, byte[] archivo);
}
