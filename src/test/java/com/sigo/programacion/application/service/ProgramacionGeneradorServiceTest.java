package com.sigo.programacion.application.service;

import com.sigo.programacion.application.port.in.GenerarProgramacionUseCase;
import com.sigo.programacion.application.port.out.ProgramacionAccessPort;
import com.sigo.programacion.application.port.out.ProgramacionGeneradorDataPort;
import com.sigo.programacion.domain.ProgramacionEstado;
import com.sigo.programacion.domain.ProgramacionGrupo;
import com.sigo.programacion.domain.ProgramacionValidationException;
import com.sigo.programacion.domain.generador.GeneradorExcepcion;
import com.sigo.programacion.domain.generador.GeneradorPlaza;
import com.sigo.programacion.domain.generador.GeneradorSecuencia;
import com.sigo.programacion.domain.generador.GeneradorTrabajador;
import com.sigo.programacion.domain.generador.GeneradorTurno;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProgramacionGeneradorServiceTest {

    private static final GeneradorPlaza PLAZA =
            new GeneradorPlaza(4L, "P4");

    @Test
    void generaCicloFullTimeSeisTrabajoDosDescanso() {
        GeneradorTrabajador trabajador =
                trabajador(10L, 1001, "Agente FT");

        StubDataPort data = new StubDataPort(
                List.of(trabajador),
                List.of(
                        new GeneradorSecuencia(
                                trabajador,
                                PLAZA,
                                ProgramacionGrupo.SECUENCIA_1,
                                1
                        )
                ),
                List.of(),
                List.of()
        );

        var resultado = service(data).generar(
                request(
                        2026,
                        9,
                        new GenerarProgramacionUseCase.CoberturaTurnosRequest(
                                1,
                                1,
                                1
                        ),
                        List.of()
                )
        );

        assertEquals(1, resultado.agentes().size());

        var dias = resultado.agentes().get(0).dias();

        assertEquals(
                YearMonth.of(2026, 9).lengthOfMonth(),
                dias.size()
        );

        long descansosPrimerBloque =
                dias.subList(0, 8)
                        .stream()
                        .filter(dia ->
                                dia.estado() == ProgramacionEstado.D
                        )
                        .count();

        assertEquals(2, descansosPrimerBloque);

        long laboralesPrimerBloque =
                dias.subList(0, 8)
                        .stream()
                        .filter(dia ->
                                dia.estado() != ProgramacionEstado.D
                        )
                        .count();

        assertEquals(6, laboralesPrimerBloque);
    }

    @Test
    void novedadEsOverlayYNoReiniciaCicloSeisPorDos() {
        GeneradorTrabajador trabajador =
                trabajador(10L, 1001, "Agente FT");

        StubDataPort data = new StubDataPort(
                List.of(trabajador),
                List.of(
                        new GeneradorSecuencia(
                                trabajador,
                                PLAZA,
                                ProgramacionGrupo.SECUENCIA_1,
                                1
                        )
                ),
                List.of(),
                List.of()
        );

        var novedad =
                new GenerarProgramacionUseCase.NovedadProgramacionRequest(
                        trabajador.getId(),
                        ProgramacionEstado.V,
                        LocalDate.of(2026, 9, 2),
                        LocalDate.of(2026, 9, 2)
                );

        var resultado = service(data).generar(
                request(
                        2026,
                        9,
                        new GenerarProgramacionUseCase.CoberturaTurnosRequest(
                                1,
                                1,
                                1
                        ),
                        List.of(novedad)
                )
        );

        Map<LocalDate, GenerarProgramacionUseCase.ProgramacionDiaPropuesta>
                porFecha = new HashMap<>();

        resultado.agentes()
                .get(0)
                .dias()
                .forEach(dia ->
                        porFecha.put(dia.fecha(), dia)
                );

        assertEquals(
                ProgramacionEstado.V,
                porFecha.get(LocalDate.of(2026, 9, 2)).estado()
        );

        assertEquals(
                ProgramacionEstado.D,
                porFecha.get(LocalDate.of(2026, 9, 7)).estado()
        );

        assertEquals(
                ProgramacionEstado.D,
                porFecha.get(LocalDate.of(2026, 9, 8)).estado()
        );

        assertNotEquals(
                ProgramacionEstado.D,
                porFecha.get(LocalDate.of(2026, 9, 9)).estado()
        );
    }

    @Test
    void excepcionQueProhibeCImpideAsignarlo() {
        GeneradorTrabajador trabajador =
                trabajador(10L, 1001, "Agente sin C");

        StubDataPort data = new StubDataPort(
                List.of(trabajador),
                List.of(
                        new GeneradorSecuencia(
                                trabajador,
                                PLAZA,
                                ProgramacionGrupo.SECUENCIA_1,
                                1
                        )
                ),
                List.of(
                        new GeneradorExcepcion(
                                trabajador,
                                PLAZA,
                                true,
                                true,
                                false,
                                "No puede realizar turno C",
                                true
                        )
                ),
                List.of()
        );

        var resultado = service(data).generar(
                request(
                        2026,
                        9,
                        new GenerarProgramacionUseCase.CoberturaTurnosRequest(
                                0,
                                0,
                                1
                        ),
                        List.of()
                )
        );

        assertTrue(
                resultado.agentes()
                        .get(0)
                        .dias()
                        .stream()
                        .noneMatch(dia ->
                                dia.estado() == ProgramacionEstado.C
                        )
        );

        assertTrue(
                resultado.cobertura()
                        .stream()
                        .anyMatch(dia -> dia.deficitC() > 0)
        );
    }

    @Test
    void partTimeNoSuperaTresDiasOperativosPorSemanaIso() {
        GeneradorTrabajador trabajador =
                trabajador(20L, 2001, "Agente PT");

        StubDataPort data = new StubDataPort(
                List.of(trabajador),
                List.of(
                        new GeneradorSecuencia(
                                trabajador,
                                PLAZA,
                                ProgramacionGrupo.PART_TIME,
                                1
                        )
                ),
                List.of(),
                List.of()
        );

        var resultado = service(data).generar(
                request(
                        2026,
                        9,
                        new GenerarProgramacionUseCase.CoberturaTurnosRequest(
                                1,
                                0,
                                0
                        ),
                        List.of()
                )
        );

        Map<String, Long> trabajadosPorSemana = new HashMap<>();

        for (var dia : resultado.agentes().get(0).dias()) {
            if (dia.estado() != null && dia.estado().esOperativo()) {
                WeekFields iso = WeekFields.ISO;
                String semana =
                        dia.fecha().get(iso.weekBasedYear())
                                + "-"
                                + dia.fecha().get(
                                        iso.weekOfWeekBasedYear()
                                );

                trabajadosPorSemana.merge(
                        semana,
                        1L,
                        Long::sum
                );
            }
        }

        assertFalse(trabajadosPorSemana.isEmpty());
        assertTrue(
                trabajadosPorSemana
                        .values()
                        .stream()
                        .allMatch(total -> total <= 3)
        );
    }

    @Test
    void rechazaDiaEspecialFueraDelMesSolicitado() {
        StubDataPort data = new StubDataPort(
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        var request =
                new GenerarProgramacionUseCase.GenerarProgramacionRequest(
                        PLAZA.id(),
                        2026,
                        9,
                        new GenerarProgramacionUseCase.CoberturaTurnosRequest(
                                1,
                                1,
                                1
                        ),
                        null,
                        List.of(
                                new GenerarProgramacionUseCase.DiaEspecialRequest(
                                        LocalDate.of(2026, 10, 1),
                                        "Fuera del mes",
                                        1,
                                        1,
                                        1
                                )
                        ),
                        List.of()
                );

        assertThrows(
                ProgramacionValidationException.class,
                () -> service(data).generar(request)
        );
    }

    private ProgramacionGeneradorService service(
            ProgramacionGeneradorDataPort data
    ) {
        return new ProgramacionGeneradorService(
                data,
                new StubAccessPort()
        );
    }

    private GenerarProgramacionUseCase.GenerarProgramacionRequest request(
            int anio,
            int mes,
            GenerarProgramacionUseCase.CoberturaTurnosRequest cobertura,
            List<GenerarProgramacionUseCase.NovedadProgramacionRequest> novedades
    ) {
        return new GenerarProgramacionUseCase.GenerarProgramacionRequest(
                PLAZA.id(),
                anio,
                mes,
                cobertura,
                null,
                List.of(),
                novedades
        );
    }

    private GeneradorTrabajador trabajador(
            Long id,
            int codigo,
            String nombre
    ) {
        return new GeneradorTrabajador(
                id,
                codigo,
                nombre,
                PLAZA
        );
    }

    private static class StubDataPort
            implements ProgramacionGeneradorDataPort {

        private final List<GeneradorTrabajador> trabajadores;
        private final List<GeneradorSecuencia> secuencias;
        private final List<GeneradorExcepcion> excepciones;
        private final List<GeneradorTurno> historial;

        private StubDataPort(
                List<GeneradorTrabajador> trabajadores,
                List<GeneradorSecuencia> secuencias,
                List<GeneradorExcepcion> excepciones,
                List<GeneradorTurno> historial
        ) {
            this.trabajadores = new ArrayList<>(trabajadores);
            this.secuencias = new ArrayList<>(secuencias);
            this.excepciones = new ArrayList<>(excepciones);
            this.historial = new ArrayList<>(historial);
        }

        @Override
        public GeneradorPlaza requirePlazaActiva(Long plazaId) {
            return PLAZA;
        }

        @Override
        public List<GeneradorTrabajador> agentesPorPlaza(Long plazaId) {
            return trabajadores;
        }

        @Override
        public List<GeneradorSecuencia> secuenciasPorPlaza(Long plazaId) {
            return secuencias;
        }

        @Override
        public List<GeneradorExcepcion> excepcionesPorPlaza(Long plazaId) {
            return excepciones;
        }

        @Override
        public List<GeneradorTurno> historial(
                Long plazaId,
                LocalDate desde,
                LocalDate hasta
        ) {
            return historial;
        }
    }

    private static class StubAccessPort
            implements ProgramacionAccessPort {

        @Override
        public Long requireSupervisorId() {
            return 1L;
        }

        @Override
        public void validarLecturaPlaza(Long plazaId) {
        }

        @Override
        public void validarGestionPlaza(Long plazaId) {
        }

        @Override
        public Long requireGestionPlazaUsuarioId(Long plazaId) {
            return 1L;
        }

        @Override
        public Long currentUserId() {
            return 1L;
        }
    }
}
