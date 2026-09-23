package com.sigo.relevo.application.service;

import com.sigo.relevo.application.port.in.ConsultarRelevosUseCase;
import com.sigo.relevo.application.port.in.RelevoHistorialUseCase;
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
public class RelevoHistorialAccesoService
        implements RelevoHistorialUseCase {

    private static final ZoneId ZONA_LIMA =
            ZoneId.of("America/Lima");

    private static final LocalTime INICIO_A =
            LocalTime.of(6, 0);

    private static final LocalTime INICIO_B =
            LocalTime.of(14, 0);

    private static final LocalTime INICIO_C =
            LocalTime.of(22, 0);

    private final ConsultarRelevosUseCase consultaUseCase;

    @Override
    public List<ConsultarRelevosUseCase.Relevo> listarPara(
            Usuario usuario,
            LocalDate inicio,
            LocalDate fin
    ) {
        if (!usuario.esOperador()) {
            return consultaUseCase.listar(inicio, fin);
        }

        exigirPlaza(usuario);
        Ventana ventana = ventanaOperativa();

        return consultaUseCase
                .listar(
                        ventana.inicio().toLocalDate(),
                        ventana.fin().toLocalDate()
                )
                .stream()
                .filter(relevo ->
                        Objects.equals(
                                relevo.plazaId(),
                                usuario.plazaId()
                        )
                )
                .filter(relevo ->
                        dentroDeVentana(relevo, ventana)
                )
                .toList();
    }

    @Override
    public ConsultarRelevosUseCase.Relevo obtenerPara(
            Usuario usuario,
            Long id
    ) {
        ConsultarRelevosUseCase.Relevo relevo =
                consultaUseCase.obtener(id);

        if (!usuario.esOperador()) {
            return relevo;
        }

        exigirPlaza(usuario);
        Ventana ventana = ventanaOperativa();

        if (!Objects.equals(
                relevo.plazaId(),
                usuario.plazaId()
        ) || !dentroDeVentana(relevo, ventana)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El operador solo puede consultar relevos de su plaza correspondientes al turno actual y al turno inmediatamente anterior"
            );
        }

        return relevo;
    }

    private boolean dentroDeVentana(
            ConsultarRelevosUseCase.Relevo relevo,
            Ventana ventana
    ) {
        if (relevo.fecha() == null || relevo.hora() == null) {
            return false;
        }

        LocalDateTime momento =
                LocalDateTime.of(
                        relevo.fecha(),
                        relevo.hora()
                );

        return !momento.isBefore(ventana.inicio())
                && !momento.isAfter(ventana.fin());
    }

    private Ventana ventanaOperativa() {
        ZonedDateTime ahoraLima =
                ZonedDateTime.now(ZONA_LIMA);

        LocalDate fecha =
                ahoraLima.toLocalDate();

        LocalTime hora =
                ahoraLima.toLocalTime();

        LocalDateTime inicio;

        if (hora.isBefore(INICIO_A)) {
            inicio = LocalDateTime.of(
                    fecha.minusDays(1),
                    INICIO_B
            );
        } else if (hora.isBefore(INICIO_B)) {
            inicio = LocalDateTime.of(
                    fecha.minusDays(1),
                    INICIO_C
            );
        } else if (hora.isBefore(INICIO_C)) {
            inicio = LocalDateTime.of(
                    fecha,
                    INICIO_A
            );
        } else {
            inicio = LocalDateTime.of(
                    fecha,
                    INICIO_B
            );
        }

        return new Ventana(
                inicio,
                ahoraLima.toLocalDateTime()
        );
    }

    private void exigirPlaza(Usuario usuario) {
        if (usuario.plazaId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El operador no tiene una plaza asignada"
            );
        }
    }

    private record Ventana(
            LocalDateTime inicio,
            LocalDateTime fin
    ) {
    }
}
