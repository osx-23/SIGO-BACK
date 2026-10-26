package com.sigo.programacion.infrastructure.report;

import com.sigo.personal.application.port.in.TrabajadorUseCase;
import com.sigo.programacion.application.port.in.DistribucionUseCase;
import com.sigo.programacion.domain.ProgramacionValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** Confirms casetas only after shifts have been programmed. */
@Service
@RequiredArgsConstructor
public class DistribucionExcelCasetasGuardarService {
    private final DistribucionExcelImportService previsualizador;
    private final DistribucionUseCase distribucion;
    private final TrabajadorUseCase trabajadores;

    @Value("${sigo.excel.importacion.guardar-habilitado:true}")
    private boolean habilitado;

    @Value("${sigo.excel.importacion.max-casetas:2000}")
    private int maxCasetas = 2000;

    public record Resultado(int guardados, int omitidos, int yaAsignadas, String mensaje) {}

    @Transactional
    public Resultado confirmar(Long plazaId, byte[] excel, int anio, int mes) {
        if (!habilitado) {
            throw new ProgramacionValidationException(
                    "Guardado de Excel deshabilitado. Activa el permiso local solo tras respaldar y revisar la base de datos.");
        }
        // Fresh read; do not accept IDs/assignments from the browser.
        var previa = previsualizador.previsualizar(plazaId, excel, anio, mes);
        if (!previa.valido()) {
            throw new ProgramacionValidationException(
                    "El Excel tiene " + previa.errores().size() + " errores; corrígelos antes de guardar casetas.");
        }
        // Respect the rule agreed with the team: casetas never create or change shifts.
        // No scheduled shift -> the preview already omits the caseta.
        // Also never assign to inactive/missing workers, even if old shifts exist.
        Map<Integer, TrabajadorUseCase.TrabajadorData> activos = new HashMap<>();
        for (var persona : trabajadores.listarAdministracion(plazaId)) {
            if (persona.codigo() != null && Boolean.TRUE.equals(persona.activo())) {
                activos.put(persona.codigo(), persona);
            }
        }
        var validos = new ArrayList<DistribucionExcelImportService.Cambio>();
        int omitidos = previa.omitidos().size();
        for (var cambio : previa.cambios()) {
            // Liberar casetas antiguas tambien es valido para un ex trabajador:
            // solo se omiten nuevas asignaciones a personal inactivo.
            if (cambio.ubicacionId() != null
                    && !activos.containsKey(cambio.codigoTrabajador())) {
                omitidos++;
                continue;
            }
            validos.add(cambio);
        }
        if (maxCasetas < 1 || validos.size() > maxCasetas) {
            throw new ProgramacionValidationException(
                    "Se intentan modificar " + validos.size() + " casetas; limite actual " + maxCasetas
                            + ". Revisa la vista previa antes de ampliar el limite.");
        }
        if (!validos.isEmpty()) {
            var datos = validos.stream()
                    .map(c -> new DistribucionUseCase.Item(c.programacionTurnoId(), c.ubicacionId(), null))
                    .toList();
            // Excel confirmado es la fuente de verdad: reemplaza distribuciones
            // manuales anteriores mediante liberaciones + asignaciones atomicas.
            // El modo forzado solo omite reglas operativas, NO validaciones
            // estructurales; los duplicados dentro del Excel ya se rechazaron.
            distribucion.guardar(new DistribucionUseCase.Command(plazaId, datos), true);
        }
        return new Resultado(validos.size(), omitidos,
                previa.yaAsignadas(), "Casetas aplicadas desde Excel (incluidas liberaciones anteriores). Los turnos no se modifican en este paso. Los cambios manuales posteriores seguiran disponibles.");
    }
}
