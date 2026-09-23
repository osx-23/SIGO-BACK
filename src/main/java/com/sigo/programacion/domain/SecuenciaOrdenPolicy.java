package com.sigo.programacion.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SecuenciaOrdenPolicy {

    public record OrdenSolicitado(
            Long agenteId,
            Integer orden
    ) {
    }

    public void validar(
            List<Long> agentesActuales,
            List<OrdenSolicitado> ordenSolicitado
    ) {
        if (agentesActuales == null || ordenSolicitado == null) {
            throw new SecuenciaOrdenInvalidoException(
                    "La secuencia enviada no es válida"
            );
        }

        if (agentesActuales.size() != ordenSolicitado.size()) {
            throw new SecuenciaOrdenInvalidoException(
                    "Debes enviar todos los integrantes de la secuencia"
            );
        }

        Set<Long> actualesIds = new HashSet<>(agentesActuales);
        Set<Long> enviadosIds = new HashSet<>();

        for (OrdenSolicitado item : ordenSolicitado) {
            if (item == null || item.agenteId() == null || item.orden() == null) {
                throw new SecuenciaOrdenInvalidoException(
                        "Cada integrante debe tener agente y posición"
                );
            }

            if (!enviadosIds.add(item.agenteId())) {
                throw new SecuenciaOrdenInvalidoException(
                        "Hay agentes repetidos en el orden enviado"
                );
            }
        }

        if (!actualesIds.equals(enviadosIds)) {
            throw new SecuenciaOrdenInvalidoException(
                    "Los agentes enviados no coinciden con los integrantes de la secuencia"
            );
        }

        Set<Integer> posiciones = new HashSet<>();

        for (OrdenSolicitado item : ordenSolicitado) {
            if (!posiciones.add(item.orden())) {
                throw new SecuenciaOrdenInvalidoException(
                        "No pueden existir posiciones repetidas"
                );
            }
        }

        for (int posicion = 1; posicion <= ordenSolicitado.size(); posicion++) {
            if (!posiciones.contains(posicion)) {
                throw new SecuenciaOrdenInvalidoException(
                        "El orden debe ser consecutivo desde 1"
                );
            }
        }
    }
}
