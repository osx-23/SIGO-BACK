package com.sigo.prediccion.application;

import com.sigo.prediccion.application.port.out.ProyeccionDatosPort;
import com.sigo.prediccion.domain.PrediccionAsistenciaParametros;
import com.sigo.prediccion.domain.PrediccionAsistenciaResultado;
import com.sigo.prediccion.domain.ProyeccionFinMesResultado;
import com.sigo.prediccion.domain.ProyeccionInvalidaException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProyeccionFinMesService {
    private final ProyeccionDatosPort datos;
    private final PrediccionAsistenciaService prediccionService;

    private record Clave(LocalDate fecha, Long turnoId) {}

    /** Solo proyecta fechas posteriores a hoy, hasta el fin del mes corriente. */
    public ProyeccionFinMesResultado proyectar(Long plazaId, Long turnoId, LocalDate hoy) {
        if (plazaId == null || plazaId <= 0 || (turnoId != null && turnoId <= 0)) {
            throw new ProyeccionInvalidaException("La plaza y el turno deben ser validos");
        }

        List<ProyeccionDatosPort.TurnoOperativo> turnos = datos.listarTurnos().stream()
                .filter(t -> t.id() != null && t.codigo() != null
                        && Set.of("A", "B", "C").contains(t.codigo().trim()))
                .filter(t -> turnoId == null || turnoId.equals(t.id()))
                .sorted(Comparator.comparing(t -> t.codigo().trim()))
                .toList();
        if (turnos.isEmpty()) {
            throw new ProyeccionInvalidaException("No hay turnos operativos para la seleccion");
        }

        LocalDate desde = hoy.plusDays(1);
        LocalDate hasta = hoy.withDayOfMonth(hoy.lengthOfMonth());
        if (desde.isAfter(hasta)) {
            return new ProyeccionFinMesResultado(plazaId, hoy, desde, hasta,
                    0, 0, 0, 0, 0, 0, List.of());
        }

        Map<LocalDate, Map<String, Set<Long>>> programados = new HashMap<>();
        for (ProyeccionDatosPort.TurnoProgramado fila : datos.programacion(plazaId, desde, hasta)) {
            if (fila.fecha() == null || fila.trabajadorId() == null
                    || fila.turnoCodigo() == null
                    || !Set.of("A", "B", "C").contains(fila.turnoCodigo())) {
                continue;
            }
            programados.computeIfAbsent(fila.fecha(), f -> new HashMap<>())
                    .computeIfAbsent(fila.turnoCodigo(), c -> new HashSet<>())
                    .add(fila.trabajadorId());
        }

        Set<Clave> registros = new HashSet<>();
        for (ProyeccionDatosPort.AsistenciaRegistrada r : datos.asistencias(plazaId, desde, hasta)) {
            if (r.fecha() != null && r.turnoId() != null) registros.add(new Clave(r.fecha(), r.turnoId()));
        }

        List<ProyeccionFinMesResultado.Detalle> detalle = new ArrayList<>();
        int totalProgramados = 0;
        int totalAusencias = 0;
        int totalCobertura = 0;
        int turnosProyectados = 0;
        int sinProgramacion = 0;
        int yaRegistrados = 0;

        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            for (ProyeccionDatosPort.TurnoOperativo turno : turnos) {
                String codigo = turno.codigo().trim();
                if (registros.contains(new Clave(fecha, turno.id()))) {
                    yaRegistrados++;
                    detalle.add(new ProyeccionFinMesResultado.Detalle(fecha, turno.id(), codigo,
                            "YA_REGISTRADO", null, null, null));
                    continue;
                }
                int cantidad = programados.getOrDefault(fecha, Map.of())
                        .getOrDefault(codigo, Set.of()).size();
                if (cantidad == 0) {
                    sinProgramacion++;
                    detalle.add(new ProyeccionFinMesResultado.Detalle(fecha, turno.id(), codigo,
                            "SIN_PROGRAMACION", null, null, null));
                    continue;
                }
                PrediccionAsistenciaResultado resultado = prediccionService.predecir(
                        new PrediccionAsistenciaParametros(plazaId, turno.id(), fecha, cantidad));
                turnosProyectados++;
                totalProgramados += cantidad;
                totalAusencias += resultado.ausenciasEstimadas();
                totalCobertura += resultado.coberturaEstimada();
                detalle.add(new ProyeccionFinMesResultado.Detalle(fecha, turno.id(), codigo,
                        "PROYECTADO", cantidad, resultado.ausenciasEstimadas(),
                        resultado.coberturaEstimada()));
            }
        }

        return new ProyeccionFinMesResultado(plazaId, hoy, desde, hasta,
                turnosProyectados, sinProgramacion, yaRegistrados,
                totalProgramados, totalAusencias, totalCobertura, List.copyOf(detalle));
    }
}
