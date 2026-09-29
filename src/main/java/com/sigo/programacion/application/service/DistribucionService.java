package com.sigo.programacion.application.service;

import com.sigo.programacion.application.port.in.DistribucionUseCase;
import com.sigo.programacion.application.port.out.DistribucionGestionPort;
import com.sigo.programacion.application.port.out.ProgramacionAccessPort;
import com.sigo.programacion.domain.ProgramacionValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DistribucionService
        implements DistribucionUseCase {

    private final DistribucionGestionPort distribucionGestionPort;
    private final ProgramacionAccessPort accessPort;

    @Override
    @Transactional(readOnly = true)
    public List<Distribucion> listar(
            Long plazaId,
            int anio,
            int mes
    ) {
        accessPort.validarGestionPlaza(plazaId);
        YearMonth periodo = yearMonth(anio, mes);

        return distribucionGestionPort.listar(
                plazaId,
                periodo.atDay(1),
                periodo.atEndOfMonth()
        );
    }

    @Override
    @Transactional
    public List<Distribucion> guardar(Command command) {
        if (command == null
                || command.plazaId() == null
                || command.distribuciones() == null
                || command.distribuciones().isEmpty()) {
            throw new ProgramacionValidationException(
                    "La distribución enviada no es válida"
            );
        }

        Long usuarioId =
                accessPort.requireGestionPlazaUsuarioId(
                        command.plazaId()
                );

        return distribucionGestionPort.guardar(
                command.plazaId(),
                command.distribuciones(),
                usuarioId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenTrabajador resumen(
            Long trabajadorId,
            int anio,
            int mes
    ) {
        Long plazaId =
                distribucionGestionPort.plazaTrabajador(
                        trabajadorId
                );

        accessPort.validarGestionPlaza(plazaId);
        YearMonth periodo = yearMonth(anio, mes);

        return distribucionGestionPort.resumen(
                trabajadorId,
                periodo.atDay(1),
                periodo.atEndOfMonth()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteTrabajador reporteTrabajador(
            Long trabajadorId,
            LocalDate desde,
            LocalDate hasta
    ) {
        if (trabajadorId == null
                || desde == null
                || hasta == null) {
            throw new ProgramacionValidationException(
                    "Agente y rango de fechas son obligatorios"
            );
        }

        if (desde.isAfter(hasta)) {
            throw new ProgramacionValidationException(
                    "La fecha desde no puede ser posterior a la fecha hasta"
            );
        }

        if (ChronoUnit.DAYS.between(desde, hasta) > 366) {
            throw new ProgramacionValidationException(
                    "El reporte no puede superar 367 días"
            );
        }

        Long plazaId =
                distribucionGestionPort.plazaTrabajador(
                        trabajadorId
                );

        accessPort.validarGestionPlaza(
                plazaId
        );

        return new ReporteTrabajador(
                distribucionGestionPort.resumen(
                        trabajadorId,
                        desde,
                        hasta
                ),
                distribucionGestionPort.listarTrabajador(
                        trabajadorId,
                        desde,
                        hasta
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ReportePlaza reportePlaza(
            Long plazaId,
            LocalDate desde,
            LocalDate hasta
    ) {
        if (
                plazaId == null
                        || desde == null
                        || hasta == null
        ) {
            throw new ProgramacionValidationException(
                    "Plaza y rango de fechas son obligatorios"
            );
        }

        if (desde.isAfter(hasta)) {
            throw new ProgramacionValidationException(
                    "La fecha desde no puede ser posterior a la fecha hasta"
            );
        }

        if (ChronoUnit.DAYS.between(desde, hasta) > 62) {
            throw new ProgramacionValidationException(
                    "El reporte matricial no puede superar 63 días"
            );
        }

        accessPort.validarGestionPlaza(
                plazaId
        );

        List<MatrizItem> items =
                distribucionGestionPort.listarMatriz(
                        plazaId,
                        desde,
                        hasta
                );

        String plazaCodigo =
                items.isEmpty()
                        ? "Plaza"
                        : items.get(0)
                                .plazaCodigo();

        return new ReportePlaza(
                plazaId,
                plazaCodigo,
                desde,
                hasta,
                items
        );
    }


    @Override
    @Transactional(readOnly = true)
    public List<CoberturaUbicacion> cobertura(
            Long plazaId,
            int anio,
            int mes
    ) {
        accessPort.validarGestionPlaza(plazaId);
        YearMonth periodo = yearMonth(anio, mes);

        return distribucionGestionPort.cobertura(
                plazaId,
                periodo.atDay(1),
                periodo.atEndOfMonth()
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
