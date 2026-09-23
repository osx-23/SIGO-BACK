package com.sigo.programacion.application.port.in;

import com.sigo.programacion.domain.ProgramacionEstado;
import com.sigo.programacion.domain.ProgramacionGrupo;

import java.time.LocalDate;
import java.util.List;

public interface GenerarProgramacionUseCase {

    ProgramacionPropuestaResponse generar(
            GenerarProgramacionRequest request
    );

    record CoberturaTurnosRequest(
            int a,
            int b,
            int c
    ) {
    }

    record DiaEspecialRequest(
            LocalDate fecha,
            String descripcion,
            int a,
            int b,
            int c
    ) {
    }

    record NovedadProgramacionRequest(
            Long trabajadorId,
            ProgramacionEstado estado,
            LocalDate desde,
            LocalDate hasta
    ) {
    }

    record GenerarProgramacionRequest(
            Long plazaId,
            int anio,
            int mes,
            CoberturaTurnosRequest coberturaNormal,
            CoberturaTurnosRequest coberturaDomingo,
            List<DiaEspecialRequest> diasEspeciales,
            List<NovedadProgramacionRequest> novedades
    ) {
    }

    record ProgramacionDiaPropuesta(
            LocalDate fecha,
            ProgramacionEstado estado,
            ProgramacionEstado estadoCiclo,
            String origen,
            boolean excepcion,
            String observacion
    ) {
    }

    record ProgramacionAgentePropuesta(
            Long trabajadorId,
            Integer codigo,
            String nombre,
            ProgramacionGrupo grupo,
            Integer orden,
            boolean partTime,
            List<ProgramacionDiaPropuesta> dias
    ) {
    }

    record CoberturaDiaResponse(
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
            String tipoCobertura
    ) {
    }

    record ConflictoProgramacionResponse(
            String tipo,
            String nivel,
            Long trabajadorId,
            Integer codigo,
            String trabajador,
            LocalDate fecha,
            String mensaje
    ) {
    }

    record ProgramacionPropuestaResponse(
            Long plazaId,
            String plazaCodigo,
            int anio,
            int mes,
            boolean guardado,
            List<ProgramacionAgentePropuesta> agentes,
            List<CoberturaDiaResponse> cobertura,
            List<ConflictoProgramacionResponse> conflictos
    ) {
    }
}
