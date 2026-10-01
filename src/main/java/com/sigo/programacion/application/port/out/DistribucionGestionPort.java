package com.sigo.programacion.application.port.out;

import com.sigo.programacion.application.port.in.DistribucionUseCase;

import java.time.LocalDate;
import java.util.List;

public interface DistribucionGestionPort {

    List<DistribucionUseCase.Distribucion> listar(
            Long plazaId,
            LocalDate desde,
            LocalDate hasta
    );

    List<DistribucionUseCase.Distribucion> guardar(
            Long plazaId,
            List<DistribucionUseCase.Item> distribuciones,
            Long usuarioId,
            boolean forzar
    );

    Long plazaTrabajador(Long trabajadorId);

    List<DistribucionUseCase.Distribucion> listarTrabajador(
            Long trabajadorId,
            LocalDate desde,
            LocalDate hasta
    );

    List<DistribucionUseCase.MatrizItem> listarMatriz(
            Long plazaId,
            LocalDate desde,
            LocalDate hasta
    );

    DistribucionUseCase.ResumenTrabajador resumen(
            Long trabajadorId,
            LocalDate desde,
            LocalDate hasta
    );

    List<DistribucionUseCase.CoberturaUbicacion> cobertura(
            Long plazaId,
            LocalDate desde,
            LocalDate hasta
    );
}
