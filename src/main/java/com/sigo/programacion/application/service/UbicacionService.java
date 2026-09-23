package com.sigo.programacion.application.service;

import com.sigo.programacion.application.port.in.UbicacionUseCase;
import com.sigo.programacion.application.port.out.ProgramacionAccessPort;
import com.sigo.programacion.application.port.out.UbicacionGestionPort;
import com.sigo.programacion.domain.ProgramacionValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UbicacionService implements UbicacionUseCase {

    private final UbicacionGestionPort ubicacionGestionPort;
    private final ProgramacionAccessPort accessPort;

    @Override
    @Transactional(readOnly = true)
    public List<Ubicacion> listarActivas(Long plazaId) {
        accessPort.validarGestionPlaza(plazaId);
        return ubicacionGestionPort.listarActivas(plazaId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ubicacion> listarConfiguracion(Long plazaId) {
        accessPort.requireSupervisorId();
        return ubicacionGestionPort.listarTodas(plazaId);
    }

    @Override
    @Transactional
    public Ubicacion crear(Command command) {
        accessPort.requireSupervisorId();
        Command normalizado = normalizar(command);

        return ubicacionGestionPort.crear(
                normalizado.plazaId(),
                normalizado.codigo(),
                normalizado.nombre(),
                normalizado.tipo(),
                normalizado.orden()
        );
    }

    @Override
    @Transactional
    public Ubicacion actualizar(
            Long ubicacionId,
            Command command
    ) {
        accessPort.requireSupervisorId();
        Command normalizado = normalizar(command);

        return ubicacionGestionPort.actualizar(
                ubicacionId,
                normalizado.plazaId(),
                normalizado.codigo(),
                normalizado.nombre(),
                normalizado.tipo(),
                normalizado.orden()
        );
    }

    @Override
    @Transactional
    public Ubicacion cambiarEstado(
            Long ubicacionId,
            boolean activo
    ) {
        accessPort.requireSupervisorId();
        return ubicacionGestionPort.cambiarEstado(
                ubicacionId,
                activo
        );
    }

    private Command normalizar(Command command) {
        if (command == null
                || command.plazaId() == null
                || command.tipo() == null) {
            throw new ProgramacionValidationException(
                    "La ubicación enviada no es válida"
            );
        }

        String codigo = command.codigo() == null
                ? ""
                : command.codigo()
                        .trim()
                        .toUpperCase(Locale.ROOT);

        String nombre = command.nombre() == null
                ? ""
                : command.nombre().trim();

        String tipo = command.tipo().trim().toUpperCase(Locale.ROOT);

        if (codigo.isBlank()) {
            throw new ProgramacionValidationException(
                    "El código de la ubicación es obligatorio"
            );
        }

        if (codigo.length() > 30) {
            throw new ProgramacionValidationException(
                    "El código de la ubicación no puede superar 30 caracteres"
            );
        }

        if (nombre.isBlank()) {
            throw new ProgramacionValidationException(
                    "El nombre de la ubicación es obligatorio"
            );
        }

        if (nombre.length() > 100) {
            throw new ProgramacionValidationException(
                    "El nombre de la ubicación no puede superar 100 caracteres"
            );
        }

        return new Command(
                command.plazaId(),
                codigo,
                nombre,
                tipo,
                command.orden()
        );
    }
}
