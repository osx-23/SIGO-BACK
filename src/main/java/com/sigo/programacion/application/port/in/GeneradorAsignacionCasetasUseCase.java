package com.sigo.programacion.application.port.in;

import com.sigo.programacion.domain.GrupoFlujoCaseta;

import java.time.LocalDate;
import java.util.List;

public interface GeneradorAsignacionCasetasUseCase {

    enum TipoPeriodo {
        SEMANA,
        MES
    }

    record Configuracion(
            Long plazaId,
            int maxMismaCasetaSemana,
            int maxMismaCasetaMes,
            int maxConsecutivos,
            boolean balancearFlujo
    ) {}

    record CasetaConfig(
            Long ubicacionId,
            String codigo,
            String nombre,
            GrupoFlujoCaseta grupoFlujo,
            Integer maxSemana,
            Integer maxMes,
            boolean activo
    ) {}

    record Restriccion(
            Long id,
            Long trabajadorId,
            Integer codigoTrabajador,
            String trabajador,
            Long ubicacionId,
            String ubicacionCodigo,
            String motivo,
            boolean activo
    ) {}

    record ItemPropuesta(
            Long programacionTurnoId,
            Long trabajadorId,
            Integer codigoTrabajador,
            String trabajador,
            LocalDate fecha,
            String turno,
            Long ubicacionId,
            String ubicacionCodigo,
            String ubicacionNombre,
            GrupoFlujoCaseta grupoFlujo,
            int puntaje
    ) {}

    record Conflicto(
            Long programacionTurnoId,
            Long trabajadorId,
            String trabajador,
            LocalDate fecha,
            String turno,
            String mensaje
    ) {}

    record Propuesta(
            Long plazaId,
            int anio,
            int mes,
            TipoPeriodo periodo,
            Integer semana,
            LocalDate desde,
            LocalDate hasta,
            List<ItemPropuesta> asignaciones,
            List<Conflicto> conflictos
    ) {}

    Configuracion obtenerConfiguracion(Long plazaId);

    Configuracion guardarConfiguracion(
            Long plazaId,
            int maxSemana,
            int maxMes,
            int maxConsecutivos,
            boolean balancearFlujo
    );

    List<CasetaConfig> listarCasetas(Long plazaId);

    CasetaConfig guardarCaseta(
            Long plazaId,
            Long ubicacionId,
            GrupoFlujoCaseta grupo,
            Integer maxSemana,
            Integer maxMes
    );

    List<Restriccion> listarRestricciones(Long plazaId);

    Restriccion guardarRestriccion(
            Long plazaId,
            Long trabajadorId,
            Long ubicacionId,
            String motivo,
            boolean activo
    );

    void eliminarRestriccion(Long restriccionId);

    Propuesta generar(
            Long plazaId,
            int anio,
            int mes,
            TipoPeriodo periodo,
            Integer semana
    );
}
