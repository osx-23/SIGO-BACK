package com.sigo.relevo.service;

import com.sigo.personal.entity.RolSistema;
import com.sigo.personal.entity.Trabajador;
import com.sigo.relevo.dto.RelevoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RelevoHistorialAccesoService {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");
    private static final LocalTime INICIO_A = LocalTime.of(6, 0);
    private static final LocalTime INICIO_B = LocalTime.of(14, 0);
    private static final LocalTime INICIO_C = LocalTime.of(22, 0);

    private final RelevoService relevoService;

    public List<RelevoResponse> listarPara(Trabajador usuario, LocalDate inicio, LocalDate fin) {
        if (usuario.getRolSistema() != RolSistema.OPERADOR) {
            return relevoService.listar(inicio, fin);
        }

        exigirPlaza(usuario);
        Ventana ventana = ventanaOperativa();

        return relevoService.listar(ventana.inicio().toLocalDate(), ventana.fin().toLocalDate()).stream()
                .filter(relevo -> Objects.equals(relevo.plazaId(), usuario.getPlaza().getId()))
                .filter(relevo -> dentroDeVentana(relevo, ventana))
                .toList();
    }

    public RelevoResponse obtenerPara(Trabajador usuario, Long id) {
        RelevoResponse relevo = relevoService.obtener(id);

        if (usuario.getRolSistema() != RolSistema.OPERADOR) {
            return relevo;
        }

        exigirPlaza(usuario);
        Ventana ventana = ventanaOperativa();

        if (!Objects.equals(relevo.plazaId(), usuario.getPlaza().getId()) || !dentroDeVentana(relevo, ventana)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El operador solo puede consultar relevos de su plaza correspondientes al turno actual y al turno inmediatamente anterior"
            );
        }

        return relevo;
    }

    private boolean dentroDeVentana(RelevoResponse relevo, Ventana ventana) {
        if (relevo.fecha() == null || relevo.hora() == null) {
            return false;
        }

        LocalDateTime momento = LocalDateTime.of(relevo.fecha(), relevo.hora());
        return !momento.isBefore(ventana.inicio()) && !momento.isAfter(ventana.fin());
    }

    private Ventana ventanaOperativa() {
        ZonedDateTime ahoraLima = ZonedDateTime.now(ZONA_LIMA);
        LocalDate fecha = ahoraLima.toLocalDate();
        LocalTime hora = ahoraLima.toLocalTime();
        LocalDateTime inicio;

        if (hora.isBefore(INICIO_A)) {
            // Turno C actual empezó ayer a las 22:00. Incluimos también el B anterior desde las 14:00.
            inicio = LocalDateTime.of(fecha.minusDays(1), INICIO_B);
        } else if (hora.isBefore(INICIO_B)) {
            // Turno A actual. El turno anterior fue C y empezó ayer a las 22:00.
            inicio = LocalDateTime.of(fecha.minusDays(1), INICIO_C);
        } else if (hora.isBefore(INICIO_C)) {
            // Turno B actual. El turno anterior fue A y empezó hoy a las 06:00.
            inicio = LocalDateTime.of(fecha, INICIO_A);
        } else {
            // Turno C actual. El turno anterior fue B y empezó hoy a las 14:00.
            inicio = LocalDateTime.of(fecha, INICIO_B);
        }

        return new Ventana(inicio, ahoraLima.toLocalDateTime());
    }

    private void exigirPlaza(Trabajador usuario) {
        if (usuario.getPlaza() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El operador no tiene una plaza asignada");
        }
    }

    private record Ventana(LocalDateTime inicio, LocalDateTime fin) {}
}
