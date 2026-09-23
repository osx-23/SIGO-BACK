package com.sigo.asistencia.application.service;

import com.sigo.asistencia.application.port.in.DashboardAsistenciaUseCase;
import com.sigo.asistencia.application.port.out.DashboardAsistenciaQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardAsistenciaService
        implements DashboardAsistenciaUseCase {

    private final DashboardAsistenciaQueryPort queryPort;

    @Override
    @Transactional(readOnly = true)
    public List<Punto> diario(
            int anio,
            int mes,
            Long plazaId,
            Long turnoId
    ) {
        validarAnio(anio);
        validarMes(mes);

        YearMonth periodo = YearMonth.of(anio, mes);

        return queryPort
                .diario(
                        periodo.atDay(1),
                        periodo.atEndOfMonth(),
                        plazaId,
                        turnoId
                )
                .stream()
                .map(this::toPunto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Punto> anual(
            int anio,
            Long plazaId,
            Long turnoId
    ) {
        validarAnio(anio);

        return queryPort
                .anual(
                        LocalDate.of(anio, 1, 1),
                        LocalDate.of(anio, 12, 31),
                        plazaId,
                        turnoId
                )
                .stream()
                .map(this::toPunto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Motivo> motivos(
            int anio,
            Integer mes,
            Long plazaId,
            Long turnoId
    ) {
        validarAnio(anio);

        LocalDate inicio;
        LocalDate fin;

        if (mes != null) {
            validarMes(mes);
            YearMonth periodo = YearMonth.of(anio, mes);
            inicio = periodo.atDay(1);
            fin = periodo.atEndOfMonth();
        } else {
            inicio = LocalDate.of(anio, 1, 1);
            fin = LocalDate.of(anio, 12, 31);
        }

        return queryPort
                .motivos(
                        inicio,
                        fin,
                        plazaId,
                        turnoId
                )
                .stream()
                .map(item ->
                        new Motivo(
                                item.motivo(),
                                item.total()
                        )
                )
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Resumen resumen(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId,
            Long turnoId
    ) {
        validarRango(inicio, fin);

        DashboardAsistenciaQueryPort.ResumenData data =
                queryPort.resumen(
                        inicio,
                        fin,
                        plazaId,
                        turnoId
                );

        return new Resumen(
                data.registros(),
                data.presentes(),
                data.programados(),
                data.ausentes(),
                calcularPorcentaje(
                        data.presentes(),
                        data.programados()
                )
        );
    }

    private Punto toPunto(
            DashboardAsistenciaQueryPort.PuntoData data
    ) {
        return new Punto(
                data.periodo(),
                data.presentes(),
                data.programados(),
                data.porcentaje()
        );
    }

    private BigDecimal calcularPorcentaje(
            long presentes,
            long programados
    ) {
        if (programados <= 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal
                .valueOf(presentes)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(programados),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private void validarMes(int mes) {
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException(
                    "El mes debe estar entre 1 y 12"
            );
        }
    }

    private void validarAnio(int anio) {
        if (anio < 2000 || anio > 2100) {
            throw new IllegalArgumentException(
                    "El año indicado no es válido"
            );
        }
    }

    private void validarRango(
            LocalDate inicio,
            LocalDate fin
    ) {
        if (inicio == null || fin == null) {
            throw new IllegalArgumentException(
                    "Las fechas inicio y fin son obligatorias"
            );
        }

        if (inicio.isAfter(fin)) {
            throw new IllegalArgumentException(
                    "La fecha de inicio no puede ser posterior a la fecha fin"
            );
        }
    }
}
