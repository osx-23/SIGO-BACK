package com.sigo.programacion.api.dto.generador;

import com.sigo.programacion.infrastructure.persistence.entity.EstadoProgramacion;
import com.sigo.programacion.infrastructure.persistence.entity.GrupoProgramacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public final class ProgramacionGeneradorDto {

    private ProgramacionGeneradorDto() {
    }

    // ============================================================
    // REQUEST
    // ============================================================

    public record CoberturaTurnosRequest(
            @Min(0) int a,
            @Min(0) int b,
            @Min(0) int c
    ) {
    }

    public record DiaEspecialRequest(
            @NotNull LocalDate fecha,
            String descripcion,

            @Min(0) int a,
            @Min(0) int b,
            @Min(0) int c
    ) {
    }

    public record NovedadProgramacionRequest(
            @NotNull Long trabajadorId,
            @NotNull EstadoProgramacion estado,
            @NotNull LocalDate desde,
            @NotNull LocalDate hasta
    ) {
    }

    public record GenerarProgramacionRequest(
            @NotNull Long plazaId,

            @Min(2020)
            @Max(2100)
            int anio,

            @Min(1)
            @Max(12)
            int mes,

            @NotNull
            @Valid
            CoberturaTurnosRequest coberturaNormal,

            @Valid
            CoberturaTurnosRequest coberturaDomingo,

            List<@Valid DiaEspecialRequest> diasEspeciales,

            List<@Valid NovedadProgramacionRequest> novedades
    ) {
    }

    // ============================================================
    // PROPUESTA POR DÍA
    // ============================================================

    public record ProgramacionDiaPropuesta(
            LocalDate fecha,

            /*
             * Estado FINAL:
             * A / B / C / D / V / COM / DM / LIC
             */
            EstadoProgramacion estado,

            /*
             * Referencia estructural.
             *
             * Para FT:
             * - D significa que el patrón 6x2 indica descanso.
             * - A significa que el patrón 6x2 indica día laboral.
             *
             * IMPORTANTE:
             * En V3 A aquí NO significa necesariamente turno A.
             * Se usa como marcador de "día laboral".
             *
             * Para PT se mantiene D porque no utiliza 6x2.
             */
            EstadoProgramacion estadoCiclo,

            /*
             * ASIGNACION_COBERTURA
             * DESCANSO_6X2
             * NOVEDAD
             * COBERTURA_PART_TIME
             * SIN_TURNO_COMPATIBLE
             */
            String origen,

            /*
             * true si existe una situación que requiere
             * atención del supervisor.
             */
            boolean excepcion,

            String observacion
    ) {
    }

    public record ProgramacionAgentePropuesta(
            Long trabajadorId,
            Integer codigo,
            String nombre,

            GrupoProgramacion grupo,
            Integer orden,

            boolean partTime,

            List<ProgramacionDiaPropuesta> dias
    ) {
    }

    // ============================================================
    // COBERTURA
    // ============================================================

    public record CoberturaDiaResponse(
            LocalDate fecha,

            int requeridoA,
            int requeridoB,
            int requeridoC,

            int asignadoA,
            int asignadoB,
            int asignadoC,

            int deficitA,
            int deficitB,
            int deficitC,

            int excesoA,
            int excesoB,
            int excesoC,

            /*
             * NORMAL
             * DOMINGO
             * ESPECIAL
             */
            String tipoCobertura
    ) {
    }

    // ============================================================
    // CONFLICTOS
    // ============================================================

    public record ConflictoProgramacionResponse(
            String tipo,
            String nivel,

            Long trabajadorId,
            Integer codigo,
            String trabajador,

            LocalDate fecha,

            String mensaje
    ) {
    }

    // ============================================================
    // RESPUESTA PRINCIPAL
    // ============================================================

    public record ProgramacionPropuestaResponse(
            Long plazaId,
            String plazaCodigo,

            int anio,
            int mes,

            /*
             * El generador solamente genera una propuesta.
             * Nunca guarda directamente.
             */
            boolean guardado,

            List<ProgramacionAgentePropuesta> agentes,

            List<CoberturaDiaResponse> cobertura,

            List<ConflictoProgramacionResponse> conflictos
    ) {
    }
}