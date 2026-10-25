package com.sigo.programacion.infrastructure.report;

import com.sigo.programacion.application.port.in.DistribucionExcelImportUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Mantiene el lector y sus validaciones; convierte el resultado al contrato de aplicación. */
@Service
@RequiredArgsConstructor
public class DistribucionExcelImportAdapter implements DistribucionExcelImportUseCase {
    private final DistribucionExcelImportService lector;

    @Override
    public VistaPrevia previsualizar(Long plazaId, byte[] archivo, Integer anio, Integer mes) {
        return convertir(lector.previsualizar(plazaId, archivo, anio, mes));
    }

    @Override
    public ExtraccionJson extraerJson(Long plazaId, byte[] archivo, int anio, int mes) {
        var o = lector.extraerJson(plazaId, archivo, anio, mes);
        return new ExtraccionJson(o.plazaId(), o.plazaCodigo(), o.desde(), o.hasta(),
                o.registros().stream().map(r -> new RegistroJson(r.codigoTrabajador(),
                        r.fecha(), r.caseta(), r.turnoExcel())).toList());
    }

    @Override
    public VistaPrevia previsualizarJson(ExtraccionJson datos) {
        var entrada = datos == null ? null : new DistribucionExcelImportService.ExtraccionJson(
                datos.plazaId(), datos.plazaCodigo(), datos.desde(), datos.hasta(),
                datos.registros() == null ? null : datos.registros().stream()
                        .map(r -> r == null ? null : new DistribucionExcelImportService.RegistroJson(
                                r.codigoTrabajador(), r.fecha(), r.caseta(), r.turnoExcel()))
                        .toList());
        return convertir(lector.previsualizarJson(entrada));
    }

    private VistaPrevia convertir(DistribucionExcelImportService.VistaPrevia o) {
        return new VistaPrevia(o.valido(), o.plazaId(), o.desde(), o.hasta(),
                o.celdasLeidas(), o.yaAsignadas(),
                o.cambios().stream().map(c -> new Cambio(c.programacionTurnoId(),
                        c.ubicacionId(), c.codigoTrabajador(), c.trabajador(),
                        c.fecha(), c.caseta())).toList(),
                o.errores(), o.omitidos());
    }
}
