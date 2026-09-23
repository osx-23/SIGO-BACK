package com.sigo.programacion.application.service;

import com.sigo.programacion.application.port.in.GuardarOrdenSecuenciaUseCase;
import com.sigo.programacion.application.port.out.SecuenciaOrdenPort;
import com.sigo.programacion.application.port.out.ProgramacionAccessPort;
import com.sigo.programacion.domain.SecuenciaOrdenInvalidoException;
import com.sigo.programacion.domain.SecuenciaOrdenPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuardarOrdenSecuenciaService
        implements GuardarOrdenSecuenciaUseCase {

    private final SecuenciaOrdenPort secuenciaOrdenPort;
    private final ProgramacionAccessPort accessPort;

    private final SecuenciaOrdenPolicy policy =
            new SecuenciaOrdenPolicy();

    @Override
    @Transactional
    public void guardar(Command command) {
        Long supervisorId =
                accessPort.requireSupervisorId();

        if (command == null
                || command.plazaId() == null
                || command.grupo() == null
                || command.agentes() == null) {
            throw new SecuenciaOrdenInvalidoException(
                    "La solicitud de orden no es válida"
            );
        }

        if (!secuenciaOrdenPort.existePlazaActiva(
                command.plazaId()
        )) {
            throw new SecuenciaOrdenInvalidoException(
                    "Plaza no válida"
            );
        }

        secuenciaOrdenPort.eliminarSecuenciasInactivas(
                command.plazaId()
        );

        var actuales =
                secuenciaOrdenPort.obtenerAgentesActivos(
                        command.plazaId(),
                        command.grupo()
                );

        var solicitados =
                command.agentes()
                        .stream()
                        .map(item ->
                                new SecuenciaOrdenPolicy.OrdenSolicitado(
                                        item.agenteId(),
                                        item.orden()
                                )
                        )
                        .toList();

        policy.validar(
                actuales,
                solicitados
        );

        secuenciaOrdenPort.reordenar(
                command.plazaId(),
                command.grupo(),
                command.agentes(),
                supervisorId
        );
    }
}
