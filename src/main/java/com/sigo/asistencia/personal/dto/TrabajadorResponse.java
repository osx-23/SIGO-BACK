package com.sigo.asistencia.personal.dto;

import com.sigo.asistencia.personal.entity.Plaza;
import com.sigo.asistencia.personal.entity.Puesto;
import com.sigo.asistencia.personal.entity.RolSistema;
import com.sigo.asistencia.personal.entity.Trabajador;

public record TrabajadorResponse(
        Long id,
        Integer codigo,
        String nombreCompleto,
        PuestoResumen puesto,
        PlazaResumen plaza,
        RolSistema rolSistema,
        Boolean requiereCambioPassword,
        Boolean activo
) {
    public static TrabajadorResponse from(Trabajador t) {
        return new TrabajadorResponse(
                t.getId(),
                t.getCodigo(),
                t.getNombreCompleto(),
                PuestoResumen.from(t.getPuesto()),
                PlazaResumen.from(t.getPlaza()),
                t.getRolSistema(),
                t.getRequiereCambioPassword(),
                t.getActivo()
        );
    }

    public record PuestoResumen(Long id, String nombre) {
        static PuestoResumen from(Puesto p) {
            return p == null ? null : new PuestoResumen(p.getId(), p.getNombre());
        }
    }

    public record PlazaResumen(Long id, String codigo, String descripcion) {
        static PlazaResumen from(Plaza p) {
            return p == null ? null : new PlazaResumen(p.getId(), p.getCodigo(), p.getDescripcion());
        }
    }
}
