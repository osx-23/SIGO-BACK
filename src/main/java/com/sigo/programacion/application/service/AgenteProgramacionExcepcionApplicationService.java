package com.sigo.programacion.application.service;

import com.sigo.programacion.application.port.in.AgenteProgramacionExcepcionUseCase;
import com.sigo.programacion.application.port.out.AgenteProgramacionExcepcionPort;
import com.sigo.programacion.domain.ProgramacionValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgenteProgramacionExcepcionApplicationService
        implements AgenteProgramacionExcepcionUseCase {

    private final AgenteProgramacionExcepcionPort port;

    @Override
    @Transactional(readOnly = true)
    public List<Excepcion> listarPorPlaza(Long plazaId) {
        validarPlaza(plazaId);
        return port.listarPorPlaza(plazaId);
    }

    @Override
    @Transactional(readOnly = true)
    public Excepcion obtener(
            Long trabajadorId,
            Long plazaId
    ) {
        validarPlaza(plazaId);
        return port.obtener(trabajadorId, plazaId);
    }

    @Override
    @Transactional
    public Excepcion guardar(Command command) {
        if (command == null
                || command.trabajadorId() == null
                || command.plazaId() == null
                || command.permiteA() == null
                || command.permiteB() == null
                || command.permiteC() == null
                || command.color() == null
                || command.color().isBlank()) {
            throw new ProgramacionValidationException(
                    "La excepción enviada no es válida"
            );
        }

        if (!Boolean.TRUE.equals(command.permiteA())
                && !Boolean.TRUE.equals(command.permiteB())
                && !Boolean.TRUE.equals(command.permiteC())) {
            throw new ProgramacionValidationException(
                    "Debe existir al menos un turno recomendado: A, B o C"
            );
        }

        validarPlaza(command.plazaId());

        if (!port.trabajadorActivoPertenecePlaza(
                command.trabajadorId(),
                command.plazaId()
        )) {
            throw new ProgramacionValidationException(
                    "El trabajador no pertenece a la plaza indicada"
            );
        }

        String motivo = limpiar(command.motivo());
        String color = command.color().trim().toUpperCase();

        return port.guardar(
                new Command(
                        command.trabajadorId(),
                        command.plazaId(),
                        command.permiteA(),
                        command.permiteB(),
                        command.permiteC(),
                        motivo,
                        color,
                        command.activo()
                )
        );
    }

    @Override
    @Transactional
    public void desactivar(
            Long trabajadorId,
            Long plazaId
    ) {
        validarPlaza(plazaId);
        port.desactivar(trabajadorId, plazaId);
    }

    private void validarPlaza(Long plazaId) {
        if (plazaId == null || !port.existePlazaActiva(plazaId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Plaza no encontrada o inactiva"
            );
        }
    }

    private String limpiar(String valor) {
        if (valor == null) {
            return null;
        }

        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
