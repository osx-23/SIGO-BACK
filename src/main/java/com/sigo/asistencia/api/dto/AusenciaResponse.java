package com.sigo.asistencia.api.dto;

public record AusenciaResponse(
        Long id, Long trabajadorId, Integer codigoTrabajador,
        String nombreTrabajador, Long motivoId, String motivo,
        String observacion, boolean excepcionPlaza
) {
    public AusenciaResponse(Long id, Long trabajadorId, Integer codigoTrabajador,
                           String nombreTrabajador, Long motivoId, String motivo,
                           String observacion) {
        this(id, trabajadorId, codigoTrabajador, nombreTrabajador,
                motivoId, motivo, observacion, false);
    }
}
