package com.sigo.relevo.application.port.in;

import java.time.LocalDate;
import java.util.List;

public interface RelevoHistorialUseCase {

    List<ConsultarRelevosUseCase.Relevo> listarPara(
            Usuario usuario,
            LocalDate inicio,
            LocalDate fin
    );

    ConsultarRelevosUseCase.Relevo obtenerPara(
            Usuario usuario,
            Long id
    );

    record Usuario(
            String rolSistema,
            Long plazaId
    ) {
        public boolean esOperador() {
            return "OPERADOR".equalsIgnoreCase(rolSistema);
        }
    }
}
