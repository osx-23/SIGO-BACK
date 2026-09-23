package com.sigo.asistencia.application.service;

import com.sigo.asistencia.application.port.out.DashboardAsistenciaQueryPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DashboardAsistenciaServiceTest {

    @Test
    void calculaPorcentajeGeneralDelResumen() {
        DashboardAsistenciaService service =
                new DashboardAsistenciaService(
                        new StubQueryPort()
                );

        var resumen = service.resumen(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                null,
                null
        );

        assertEquals(
                new BigDecimal("75.00"),
                resumen.porcentajeGeneral()
        );
    }

    @Test
    void rechazaMesFueraDeRango() {
        DashboardAsistenciaService service =
                new DashboardAsistenciaService(
                        new StubQueryPort()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.diario(
                        2026,
                        13,
                        null,
                        null
                )
        );
    }

    private static class StubQueryPort
            implements DashboardAsistenciaQueryPort {

        @Override
        public List<PuntoData> diario(
                LocalDate inicio,
                LocalDate fin,
                Long plazaId,
                Long turnoId
        ) {
            return List.of();
        }

        @Override
        public List<PuntoData> anual(
                LocalDate inicio,
                LocalDate fin,
                Long plazaId,
                Long turnoId
        ) {
            return List.of();
        }

        @Override
        public List<MotivoData> motivos(
                LocalDate inicio,
                LocalDate fin,
                Long plazaId,
                Long turnoId
        ) {
            return List.of();
        }

        @Override
        public ResumenData resumen(
                LocalDate inicio,
                LocalDate fin,
                Long plazaId,
                Long turnoId
        ) {
            return new ResumenData(
                    5L,
                    15L,
                    20L,
                    5L
            );
        }
    }
}
