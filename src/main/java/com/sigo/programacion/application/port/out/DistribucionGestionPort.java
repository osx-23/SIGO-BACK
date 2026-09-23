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
            Long usuarioId
    );

    Long plazaTrabajador(Long trabajadorId);

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
