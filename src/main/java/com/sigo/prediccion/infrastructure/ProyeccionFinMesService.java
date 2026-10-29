package com.sigo.prediccion.infrastructure;

import com.sigo.asistencia.infrastructure.persistence.entity.AsistenciaRegistro;
import com.sigo.asistencia.infrastructure.persistence.repository.AsistenciaRegistroRepository;
import com.sigo.personal.infrastructure.persistence.entity.Turno;
import com.sigo.personal.infrastructure.persistence.repository.TurnoRepository;
import com.sigo.prediccion.api.dto.ProyeccionFinMesResponse;
import com.sigo.prediccion.application.PrediccionAsistenciaService;
import com.sigo.prediccion.domain.PrediccionAsistenciaParametros;
import com.sigo.prediccion.domain.PrediccionAsistenciaResultado;
import com.sigo.programacion.infrastructure.persistence.entity.EstadoProgramacion;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionTurnoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProyeccionFinMesService {
    private final ProgramacionTurnoRepository programacionRepository;
    private final AsistenciaRegistroRepository asistenciaRepository;
    private final TurnoRepository turnoRepository;
    private final PrediccionAsistenciaService prediccionService;

    private record Clave(LocalDate fecha, Long turnoId) {}

    /**
     * Proyecta solo fechas posteriores a hoy (zona horaria del negocio definida en controller).
     * Nunca supone una cantidad de programados cuando no existe programacion.
     */
    public ProyeccionFinMesResponse proyectar(Long plazaId, Long turnoId, LocalDate hoy) {
        if (plazaId == null || plazaId <= 0 || (turnoId != null && turnoId <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La plaza y el turno deben ser validos");
        }

        List<Turno> turnos = turnoRepository.findAll().stream()
                .filter(t -> t.getCodigo() != null && Set.of("A", "B", "C").contains(t.getCodigo().trim()))
                .filter(t -> turnoId == null || turnoId.equals(t.getId()))
                .sorted(Comparator.comparing(t -> t.getCodigo().trim()))
                .toList();
        if (turnos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay turnos operativos para la seleccion");
        }

        LocalDate desde = hoy.plusDays(1);
        LocalDate hasta = hoy.withDayOfMonth(hoy.lengthOfMonth());
        if (desde.isAfter(hasta)) {
            return new ProyeccionFinMesResponse(plazaId, hoy, desde, hasta,
                    0, 0, 0, 0, 0, 0, List.of());
        }

        // Unica lectura de programacion para el rango, sin consultar por trabajador en un bucle.
        Map<LocalDate, Map<EstadoProgramacion, Set<Long>>> programados = new HashMap<>();
        for (ProgramacionTurnoRepository.TurnoResumen fila :
                programacionRepository.findMesResumen(plazaId, desde, hasta)) {
            if (fila.getFecha() == null || fila.getTrabajadorId() == null
                    || fila.getEstado() == null || !fila.getEstado().esOperativo()) {
                continue; // D, V, COM, DM, LIC no son turnos de trabajo.
            }
            programados.computeIfAbsent(fila.getFecha(), f -> new EnumMap<>(EstadoProgramacion.class))
                    .computeIfAbsent(fila.getEstado(), e -> new HashSet<>())
                    .add(fila.getTrabajadorId());
        }

        // Los turnos que ya tienen asistencia real nunca se vuelven a pronosticar.
        Set<Clave> registrosExistentes = new HashSet<>();
        for (AsistenciaRegistro registro :
                asistenciaRepository.findByFechaBetweenAndPlazaIdOrderByFechaDescIdDesc(desde, hasta, plazaId)) {
            if (registro.getTurno() != null) {
                registrosExistentes.add(new Clave(registro.getFecha(), registro.getTurno().getId()));
            }
        }

        List<ProyeccionFinMesResponse.Detalle> detalle = new ArrayList<>();
        int totalProgramados = 0;
        int totalAusencias = 0;
        int totalCobertura = 0;
        int turnosProyectados = 0;
        int sinProgramacion = 0;
        int yaRegistrados = 0;

        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            for (Turno turno : turnos) {
                String codigo = turno.getCodigo().trim();
                EstadoProgramacion estado = EstadoProgramacion.valueOf(codigo);
                if (registrosExistentes.contains(new Clave(fecha, turno.getId()))) {
                    yaRegistrados++;
                    detalle.add(new ProyeccionFinMesResponse.Detalle(fecha, turno.getId(), codigo,
                            "YA_REGISTRADO", null, null, null));
                    continue;
                }
                int cantidad = programados.getOrDefault(fecha, Map.of())
                        .getOrDefault(estado, Set.of()).size();
                if (cantidad == 0) {
                    sinProgramacion++;
                    detalle.add(new ProyeccionFinMesResponse.Detalle(fecha, turno.getId(), codigo,
                            "SIN_PROGRAMACION", null, null, null));
                    continue;
                }
                PrediccionAsistenciaResultado resultado = prediccionService.predecir(
                        new PrediccionAsistenciaParametros(plazaId, turno.getId(), fecha, cantidad));
                turnosProyectados++;
                totalProgramados += cantidad;
                totalAusencias += resultado.ausenciasEstimadas();
                totalCobertura += resultado.coberturaEstimada();
                detalle.add(new ProyeccionFinMesResponse.Detalle(fecha, turno.getId(), codigo,
                        "PROYECTADO", cantidad, resultado.ausenciasEstimadas(),
                        resultado.coberturaEstimada()));
            }
        }

        return new ProyeccionFinMesResponse(plazaId, hoy, desde, hasta,
                turnosProyectados, sinProgramacion, yaRegistrados,
                totalProgramados, totalAusencias, totalCobertura, List.copyOf(detalle));
    }
}
