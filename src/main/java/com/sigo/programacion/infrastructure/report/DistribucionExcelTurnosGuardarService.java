package com.sigo.programacion.infrastructure.report;

import com.sigo.personal.application.port.in.TrabajadorUseCase;
import com.sigo.programacion.application.port.in.GuardarTurnosUseCase;
import com.sigo.programacion.domain.ProgramacionValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Saves reviewed turn differences through SIGO's existing authenticated use case. */
@Service
@RequiredArgsConstructor
public class DistribucionExcelTurnosGuardarService {
    private final DistribucionExcelTurnosPreviewService previsualizador;
    private final TrabajadorUseCase trabajadores;
    private final GuardarTurnosUseCase guardarTurnos;

    @Value("${sigo.excel.importacion.guardar-habilitado:true}")
    private boolean habilitado;

    // Prevent unintended massive changes in the real database.
    @Value("${sigo.excel.importacion.max-turnos:1200}")
    private int maxTurnos = 1200;

    public record Resultado(int guardados, int omitidos, String mensaje) {}

    @Transactional
    public Resultado confirmar(Long plazaId, byte[] excel, int anio, int mes) {
        if (!habilitado) {
            throw new ProgramacionValidationException(
                    "Guardado de Excel deshabilitado. Activa el permiso local solo tras respaldar y revisar la base de datos.");
        }
        // Read live DB again to avoid blindly trusting a previous frontend preview.
        var previa = previsualizador.previsualizar(plazaId, excel, anio, mes);
        Map<Integer, TrabajadorUseCase.TrabajadorData> personas = new HashMap<>();
        for (var trabajador : trabajadores.listarAdministracion(plazaId)) {
            if (trabajador.codigo() != null) personas.put(trabajador.codigo(), trabajador);
        }
        List<GuardarTurnosUseCase.Item> guardables = new ArrayList<>();
        int omitidos = 0;
        Set<String> estadosExcel = Set.of("A", "B", "C", "D", "DM", "V", "COM", "LIC");
        for (var p : previa.propuestas()) {
            if (!"CREAR".equals(p.accion()) && !"ACTUALIZAR".equals(p.accion())) {
                if ("REVISAR".equals(p.accion())) omitidos++;
                continue;
            }
            // El Excel es autoridad para las celdas explicitas A/B/C/D/DM/V/COM/LIC.
            // Si A->DM o DM->A, se aplica el cambio confirmado. Un DM sin
            // cambio de estado puede requerir limpiar una antigua caseta.
            if (!estadosExcel.contains(p.turnoExcel())) {
                omitidos++;
                continue;
            }
            if (p.turnoExcel().equals(p.turnoSigo())
                    && (Set.of("A", "B", "C").contains(p.turnoExcel())
                        || p.casetaSigo() == null || p.casetaSigo().isBlank())) continue;
            var persona = personas.get(p.codigoTrabajador());
            if (persona == null || !Boolean.TRUE.equals(persona.activo())) {
                omitidos++;
                continue;
            }
            guardables.add(new GuardarTurnosUseCase.Item(persona.id(), p.fecha(), p.turnoExcel()));
        }
        if (maxTurnos < 1 || guardables.size() > maxTurnos) {
            throw new ProgramacionValidationException(
                    "Se intentan modificar " + guardables.size() + " turnos; limite actual " + maxTurnos
                            + ". Revisa la vista previa antes de ampliar el limite.");
        }
        if (!guardables.isEmpty()) {
            // Existing use case enforces supervisor permissions and performs the DB write.
            guardarTurnos.guardar(new GuardarTurnosUseCase.Command(plazaId, List.copyOf(guardables)));
        }
        return new Resultado(guardables.size(), omitidos,
                "Programacion aplicada desde Excel (A/B/C/D/DM/V/COM/LIC). Puedes editarla manualmente despues; otra importacion la reemplazara.");
    }
}
