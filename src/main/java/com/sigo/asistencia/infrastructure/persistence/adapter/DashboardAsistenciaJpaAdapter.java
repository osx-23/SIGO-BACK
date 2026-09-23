package com.sigo.asistencia.infrastructure.persistence.adapter;

import com.sigo.asistencia.application.port.out.DashboardAsistenciaQueryPort;
import com.sigo.asistencia.infrastructure.persistence.repository.AsistenciaAusenciaRepository;
import com.sigo.asistencia.infrastructure.persistence.repository.AsistenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DashboardAsistenciaJpaAdapter
        implements DashboardAsistenciaQueryPort {

    private final AsistenciaRepository asistenciaRepository;
    private final AsistenciaAusenciaRepository ausenciaRepository;

    @Override
    public List<PuntoData> diario(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId,
            Long turnoId
    ) {
        return asistenciaRepository
                .obtenerDiario(
                        inicio,
                        fin,
                        plazaId,
                        turnoId
                )
                .stream()
                .map(this::toPunto)
                .toList();
    }

    @Override
    public List<PuntoData> anual(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId,
            Long turnoId
    ) {
        return asistenciaRepository
                .obtenerAnual(
                        inicio,
                        fin,
                        plazaId,
                        turnoId
                )
                .stream()
                .map(this::toPunto)
                .toList();
    }

    @Override
    public List<MotivoData> motivos(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId,
            Long turnoId
    ) {
        return ausenciaRepository
                .contarPorMotivo(
                        inicio,
                        fin,
                        plazaId,
                        turnoId
                )
                .stream()
                .map(fila ->
                        new MotivoData(
                                String.valueOf(valor(fila, 0)),
                                numero(valor(fila, 1)).longValue()
                        )
                )
                .toList();
    }

    @Override
    public ResumenData resumen(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId,
            Long turnoId
    ) {
        List<Object[]> resultado =
                asistenciaRepository.obtenerResumen(
                        inicio,
                        fin,
                        plazaId,
                        turnoId
                );

        if (resultado == null || resultado.isEmpty()) {
            return new ResumenData(
                    0L,
                    0L,
                    0L,
                    0L
            );
        }

        Object[] fila = resultado.get(0);

        return new ResumenData(
                numero(valor(fila, 0)).longValue(),
                numero(valor(fila, 1)).longValue(),
                numero(valor(fila, 2)).longValue(),
                numero(valor(fila, 3)).longValue()
        );
    }

    private PuntoData toPunto(Object[] fila) {
        return new PuntoData(
                numero(valor(fila, 0)).intValue(),
                numero(valor(fila, 1)).longValue(),
                numero(valor(fila, 2)).longValue(),
                decimal(valor(fila, 3))
        );
    }

    private Object valor(Object[] fila, int indice) {
        if (fila == null
                || indice < 0
                || indice >= fila.length) {
            return null;
        }

        return fila[indice];
    }

    private Number numero(Object valor) {
        if (valor == null) {
            return 0;
        }

        if (valor instanceof Number numero) {
            return numero;
        }

        try {
            return new BigDecimal(valor.toString());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private BigDecimal decimal(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }

        if (valor instanceof BigDecimal decimal) {
            return decimal;
        }

        if (valor instanceof Number numero) {
            return new BigDecimal(numero.toString());
        }

        try {
            return new BigDecimal(valor.toString());
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }
}
