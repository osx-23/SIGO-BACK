package com.sigo.programacion.application.service;

import com.sigo.programacion.application.port.in.GuardarTurnosUseCase;
import com.sigo.programacion.application.port.in.ListarTurnosUseCase;
import com.sigo.programacion.application.port.out.ProgramacionAccessPort;
import com.sigo.programacion.application.port.out.TurnoGestionPort;
import com.sigo.programacion.domain.ProgramacionValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TurnoService
        implements ListarTurnosUseCase, GuardarTurnosUseCase {

    private final TurnoGestionPort turnoGestionPort;
    private final ProgramacionAccessPort accessPort;

    @Override
    @Transactional(readOnly = true)
    public List<Turno> listar(
            Long plazaId,
            int anio,
            int mes
    ) {
        accessPort.validarLecturaPlaza(plazaId);

        YearMonth periodo = yearMonth(anio, mes);

        return turnoGestionPort.listar(
                plazaId,
                periodo.atDay(1),
                periodo.atEndOfMonth()
        );
    }

    @Override
    @Transactional
    public List<Turno> guardar(Command command) {
        Long supervisorId = accessPort.requireSupervisorId();

        if (command == null
                || command.plazaId() == null
                || command.programaciones() == null
                || command.programaciones().isEmpty()) {
            throw new ProgramacionValidationException(
                    "La programación enviada no es válida"
            );
        }

        return turnoGestionPort.guardar(
                command.plazaId(),
                command.programaciones(),
                supervisorId
        );
    }

    private YearMonth yearMonth(int anio, int mes) {
        try {
            return YearMonth.of(anio, mes);
        } catch (Exception exception) {
            throw new ProgramacionValidationException(
                    "Año o mes inválido"
            );
        }
    }
}
