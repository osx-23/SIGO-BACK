package com.sigo.programacion.application.service;

import com.sigo.programacion.application.port.in.AsignarLiderUseCase;
import com.sigo.programacion.application.port.in.ListarLideresUseCase;
import com.sigo.programacion.application.port.out.LiderGestionPort;
import com.sigo.programacion.application.port.out.ProgramacionAccessPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LiderService
        implements ListarLideresUseCase, AsignarLiderUseCase {

    private final LiderGestionPort liderGestionPort;
    private final ProgramacionAccessPort accessPort;

    @Override
    @Transactional(readOnly = true)
    public List<Lider> listar(Long plazaId) {
        accessPort.requireSupervisorId();
        return liderGestionPort.listar(plazaId);
    }

    @Override
    @Transactional
    public Lider asignar(Command command) {
        Long supervisorId = accessPort.requireSupervisorId();

        if (command == null
                || command.agenteId() == null
                || command.controladorId() == null
                || command.plazaId() == null) {
            throw new IllegalArgumentException(
                    "La solicitud de líder no es válida"
            );
        }

        LocalDate inicio = command.fechaInicio() == null
                ? LocalDate.now()
                : command.fechaInicio();

        return liderGestionPort.asignar(
                command.agenteId(),
                command.controladorId(),
                command.plazaId(),
                inicio,
                supervisorId
        );
    }
}
