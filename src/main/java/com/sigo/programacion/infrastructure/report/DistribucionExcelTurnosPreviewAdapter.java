package com.sigo.programacion.infrastructure.report;

import com.sigo.programacion.application.port.in.DistribucionExcelTurnosPreviewUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Traduce las propuestas del importador de turnos al contrato de aplicación. */
@Service
@RequiredArgsConstructor
public class DistribucionExcelTurnosPreviewAdapter implements DistribucionExcelTurnosPreviewUseCase {
    private final DistribucionExcelTurnosPreviewService lector;

    @Override
    public VistaPrevia previsualizar(Long plazaId, byte[] archivo, Integer anio, Integer mes) {
        var o = lector.previsualizar(plazaId, archivo, anio, mes);
        return new VistaPrevia(o.desde(), o.hasta(), o.leidas(),
                o.crear(), o.actualizar(), o.coinciden(), o.revisar(),
                o.propuestas().stream().map(p -> new Propuesta(p.codigoTrabajador(),
                        p.trabajador(), p.fecha(), p.valorExcel(), p.turnoExcel(),
                        p.turnoSigo(), p.casetaSigo(), p.accion(), p.detalle())).toList(),
                o.advertencias());
    }
}
