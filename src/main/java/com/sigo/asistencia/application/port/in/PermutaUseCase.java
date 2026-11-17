package com.sigo.asistencia.application.port.in;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Contrato de permutas; no depende de adaptadores ni de HTTP. */
public interface PermutaUseCase {
    record PermutaCommand(
            Integer codigoSolicitante, LocalDate fechaSolicitante, Long turnoSolicitanteId,
            Long plazaSolicitanteId, Integer codigoAceptante, LocalDate fechaAceptante,
            Long turnoAceptanteId, Long plazaAceptanteId
    ) {}

    record AgenteIdentificado(int codigo, String nombre) {}

    record PermutaDetalle(
            Long id, Long plazaSolicitanteId, String plazaSolicitante,
            int codigoSolicitante, String nombreSolicitante,
            Long turnoSolicitanteId, String turnoSolicitante, LocalDate fechaSolicitante,
            Long plazaAceptanteId, String plazaAceptante,
            int codigoAceptante, String nombreAceptante,
            Long turnoAceptanteId, String turnoAceptante, LocalDate fechaAceptante
    ) {}
    record TotalPlaza(String plaza, long total) {}
    record TopAgente(int codigo, String nombre, long total) {}
    record DashboardPermutas(long total, List<TotalPlaza> porPlaza, List<TopAgente> topAgentes) {}
    record ResumenMensual(int mes, long total, List<TotalPlaza> porPlaza) {}
    record ResumenAnualPermutas(
            int anio, long total, List<String> plazas,
            List<ResumenMensual> meses, List<TotalPlaza> totalesPorPlaza,
            List<TopAgente> topAgentes) {}
    record PaginaPermutas(List<PermutaDetalle> registros, long total,
                          int pagina, int tamano, long totalPaginas) {}


    AgenteIdentificado agente(int codigo);
    void registrarDesdeAsistencia(Long asistenciaId, Long plazaId, Long turnoId,
                                  LocalDate fecha, List<PermutaCommand> solicitudes);
    List<PermutaDetalle> listar(LocalDate inicio, LocalDate fin,
                               Long plazaId, Integer codigo, Long turnoId);
    PaginaPermutas listarPaginado(LocalDate inicio, LocalDate fin,
                                 Long plazaId, Integer codigo, Long turnoId,
                                 int pagina, int tamano);
    List<PermutaDetalle> delTurno(Long plazaId, Long turnoId, LocalDate fecha);
    DashboardPermutas dashboard(YearMonth mes);
    ResumenAnualPermutas resumenAnual(int anio);
}
