package com.sigo.asistencia.application.service;

import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Turno;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.TurnoRepository;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AsistenciaProgramacionService {

    private final PlazaRepository plazaRepository;
    private final TurnoRepository turnoRepository;

    public int obtenerProgramados(Long plazaId, Long turnoId) {
        Plaza plaza = plazaRepository.findById(plazaId)
                .orElseThrow(() -> new ResourceNotFoundException("Plaza no encontrada"));
        Turno turno = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado"));

        String plazaCodigo = normalizar(plaza.getCodigo());
        String turnoCodigo = normalizar(turno.getCodigo());

        return switch (plazaCodigo) {
            case "P1" -> porTurno(turnoCodigo, 17, 17, 9);
            case "P2", "P3", "P2 Y P3" -> porTurno(turnoCodigo, 25, 25, 11);
            case "P4", "P5" -> porTurno(turnoCodigo, 7, 7, 3);
            case "P6", "P7", "P6 Y P7" -> porTurno(turnoCodigo, 13, 12, 5);
            case "P8" -> porTurno(turnoCodigo, 10, 12, 4);
            case "P9" -> porTurno(turnoCodigo, 2, 2, 2);
            case "P10" -> porTurno(turnoCodigo, 12, 12, 4);
            default -> throw new BusinessException("No existe programación de agentes para la plaza " + plazaCodigo);
        };
    }

    private String normalizar(String valor) {
        return valor == null
                ? ""
                : valor.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private int porTurno(String turno, int a, int b, int c) {
        return switch (turno) {
            case "A" -> a;
            case "B" -> b;
            case "C" -> c;
            default -> throw new BusinessException("Turno no válido para programación de asistencia: " + turno);
        };
    }
}
