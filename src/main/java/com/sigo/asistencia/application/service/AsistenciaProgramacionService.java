package com.sigo.asistencia.application.service;

import com.sigo.asistencia.application.port.in.ObtenerProgramadosAsistenciaUseCase;
import com.sigo.asistencia.application.port.out.AsistenciaProgramacionCatalogoPort;
import com.sigo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AsistenciaProgramacionService
        implements ObtenerProgramadosAsistenciaUseCase {

    private final AsistenciaProgramacionCatalogoPort catalogoPort;

    @Override
    public int obtenerProgramados(
            Long plazaId,
            Long turnoId
    ) {
        String plazaCodigo =
                normalizar(
                        catalogoPort.requirePlazaCodigo(plazaId)
                );

        String turnoCodigo =
                normalizar(
                        catalogoPort.requireTurnoCodigo(turnoId)
                );

        return switch (plazaCodigo) {
            case "P1" ->
                    porTurno(turnoCodigo, 17, 17, 9);

            case "P2", "P3", "P2 Y P3" ->
                    porTurno(turnoCodigo, 25, 25, 11);

            case "P4", "P5" ->
                    porTurno(turnoCodigo, 7, 7, 3);

            case "P6", "P7", "P6 Y P7" ->
                    porTurno(turnoCodigo, 13, 12, 5);

            case "P8" ->
                    porTurno(turnoCodigo, 10, 12, 4);

            case "P9" ->
                    porTurno(turnoCodigo, 2, 2, 2);

            case "P10" ->
                    porTurno(turnoCodigo, 12, 12, 4);

            default ->
                    throw new BusinessException(
                            "No existe programación de agentes para la plaza "
                                    + plazaCodigo
                    );
        };
    }

    private String normalizar(String valor) {
        return valor == null
                ? ""
                : valor
                        .trim()
                        .replaceAll("\\s+", " ")
                        .toUpperCase(Locale.ROOT);
    }

    private int porTurno(
            String turno,
            int a,
            int b,
            int c
    ) {
        return switch (turno) {
            case "A" -> a;
            case "B" -> b;
            case "C" -> c;
            default ->
                    throw new BusinessException(
                            "Turno no válido para programación de asistencia: "
                                    + turno
                    );
        };
    }
}
