package com.sigo.programacion.application.port.out;

import com.sigo.programacion.domain.TrasladoPlaza;
import java.time.LocalDate;
import java.util.List;

public interface TrasladoPlazaPort {
    record Candidato(Long id, Integer codigo, String nombre, String puesto, Long plazaId, String plazaCodigo) {}
    List<Candidato> candidatos();
    List<TrasladoPlaza> listar(Long plazaId, LocalDate desde, LocalDate hasta);
    TrasladoPlaza crear(Long trabajadorId, Long destinoId, LocalDate inicio, LocalDate fin, Long supervisorId);
    void cancelar(Long id);
}
