package com.sigo.programacion.infrastructure.report;

import com.sigo.programacion.application.port.in.ProgramacionExcelTextoUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Expone el importador de texto a través de aplicación sin modificar su lógica. */
@Service
@RequiredArgsConstructor
public class ProgramacionExcelTextoAdapter implements ProgramacionExcelTextoUseCase {
    private final ProgramacionExcelTextoService lector;

    @Override
    public VistaPrevia previsualizar(Long plazaId, int anio, int mes, byte[] archivo) {
        var o = lector.previsualizar(plazaId, anio, mes, archivo);
        return new VistaPrevia(o.leidos(), o.coinciden(),
                o.cambios().stream().map(c -> new Cambio(c.trabajadorId(),
                        c.codigoTrabajador(), c.nombre(), c.fecha(), c.anterior(),
                        c.nuevo())).toList(), o.errores(), o.omitidos());
    }
}
