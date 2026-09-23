package com.sigo.programacion.application.service;

import com.sigo.programacion.application.port.in.AsignarSecuenciaUseCase;
import com.sigo.programacion.application.port.in.ListarSecuenciasUseCase;
import com.sigo.programacion.application.port.out.ProgramacionAccessPort;
import com.sigo.programacion.application.port.out.SecuenciaGestionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SecuenciaService
        implements ListarSecuenciasUseCase, AsignarSecuenciaUseCase {

    private final SecuenciaGestionPort secuenciaGestionPort;
    private final ProgramacionAccessPort accessPort;

    @Override
    @Transactional(readOnly = true)
    public List<Secuencia> listar(Long plazaId) {
        accessPort.validarLecturaPlaza(plazaId);
        return secuenciaGestionPort.listar(plazaId);
    }

    @Override
    @Transactional
    public Secuencia asignar(Command command) {
        Long supervisorId = accessPort.requireSupervisorId();

        if (command == null
                || command.agenteId() == null
                || command.plazaId() == null
                || command.grupo() == null
                || command.grupo().isBlank()) {
            throw new IllegalArgumentException(
                    "La solicitud de secuencia no es válida"
            );
        }

        return secuenciaGestionPort.asignar(
                command.agenteId(),
                command.plazaId(),
                command.grupo(),
                supervisorId
        );
    }
}
