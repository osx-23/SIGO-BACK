package com.sigo.asistencia.application.service;

import com.sigo.asistencia.application.port.in.ConsultarAsistenciasUseCase;
import com.sigo.asistencia.application.port.in.GestionarAsistenciaUseCase;
import com.sigo.asistencia.application.port.in.RegistrarAsistenciaExcepcionUseCase;
import com.sigo.asistencia.application.port.out.AsistenciaExcepcionPort;
import com.sigo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RegistrarAsistenciaExcepcionService
        implements RegistrarAsistenciaExcepcionUseCase {

    private final AsistenciaExcepcionPort excepcionPort;
    private final ConsultarAsistenciasUseCase consultaUseCase;

    @Override
    @Transactional
    public ConsultarAsistenciasUseCase.Asistencia registrar(
            GestionarAsistenciaUseCase.Command command
    ) {
        GestionarAsistenciaUseCase.Command validado =
                validar(command);

        Long id = excepcionPort.registrar(validado);

        return consultaUseCase.obtenerPorId(id);
    }

    private GestionarAsistenciaUseCase.Command validar(
            GestionarAsistenciaUseCase.Command command
    ) {
        if (command == null
                || command.plazaId() == null
                || command.turnoId() == null
                || command.controladorId() == null
                || command.fecha() == null
                || command.programados() == null
                || command.presentes() == null) {
            throw new BusinessException(
                    "Los datos de asistencia son obligatorios"
            );
        }

        int programados = command.programados();
        int presentes = command.presentes();
        int apoyo = command.apoyoSolicitado() == null
                ? 0
                : command.apoyoSolicitado();

        if (programados < 0) {
            throw new BusinessException(
                    "La cantidad de programados no puede ser negativa"
            );
        }

        if (presentes < 0 || presentes > programados) {
            throw new BusinessException(
                    "Los presentes deben estar entre 0 y "
                            + programados
            );
        }

        if (apoyo < 0) {
            throw new BusinessException(
                    "El apoyo solicitado no puede ser negativo"
            );
        }

        List<GestionarAsistenciaUseCase.AusenciaCommand> ausencias =
                command.ausencias() == null
                        ? List.of()
                        : command.ausencias();

        int esperadas = programados - presentes;

        if (ausencias.size() != esperadas) {
            throw new BusinessException(
                    "Debe registrar exactamente "
                            + esperadas
                            + " ausencia(s)"
            );
        }

        Set<Long> trabajadores = new HashSet<>();

        for (GestionarAsistenciaUseCase.AusenciaCommand ausencia
                : ausencias) {
            if (ausencia == null
                    || ausencia.trabajadorId() == null
                    || ausencia.motivoId() == null) {
                throw new BusinessException(
                        "La ausencia contiene datos incompletos"
                );
            }

            if (!trabajadores.add(ausencia.trabajadorId())) {
                throw new BusinessException(
                        "No puede registrar al mismo trabajador ausente dos veces"
                );
            }
        }

        return new GestionarAsistenciaUseCase.Command(
                command.plazaId(),
                command.turnoId(),
                command.controladorId(),
                command.fecha(),
                programados,
                presentes,
                apoyo,
                apoyo > 0
                        ? limpiar(command.detalleApoyo())
                        : null,
                limpiar(command.notas()),
                ausencias,
                command.evidencias() == null
                        ? List.of()
                        : command.evidencias()
        );
    }

    private String limpiar(String texto) {
        if (texto == null) {
            return null;
        }

        String limpio = texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
