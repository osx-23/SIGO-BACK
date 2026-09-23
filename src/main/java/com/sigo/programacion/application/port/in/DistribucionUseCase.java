package com.sigo.programacion.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DistribucionUseCase {

    List<Distribucion> listar(Long plazaId, int anio, int mes);

    List<Distribucion> guardar(Command command);

    ResumenTrabajador resumen(Long trabajadorId, int anio, int mes);

    List<CoberturaUbicacion> cobertura(Long plazaId, int anio, int mes);

    record Command(
            Long plazaId,
            List<Item> distribuciones
    ) {
    }

    record Item(
            Long programacionTurnoId,
            Long ubicacionId,
            String observacion
    ) {
    }

    record Distribucion(
            Long distribucionId,
            Long programacionTurnoId,
            Long trabajadorId,
            Integer codigoTrabajador,
            String nombreTrabajador,
            LocalDate fecha,
            String estado,
            Long ubicacionId,
            String ubicacionCodigo,
            String ubicacionNombre,
            String ubicacionTipo,
            String observacion
    ) {
    }

    record ResumenUbicacion(
            String codigo,
            String nombre,
            long veces
    ) {
    }

    record ResumenTrabajador(
            Long trabajadorId,
            Integer codigo,
            String nombre,
            List<ResumenUbicacion> ubicaciones
    ) {
    }

    record CoberturaUbicacion(
            Long ubicacionId,
            String codigo,
            String nombre,
            Map<LocalDate, Long> porDia
    ) {
    }
}
