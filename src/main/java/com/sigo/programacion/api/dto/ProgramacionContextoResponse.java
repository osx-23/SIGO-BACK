package com.sigo.programacion.api.dto;

import com.sigo.personal.api.dto.TrabajadorPublicResponse;

import java.util.List;

/**
 * Contexto de solo lectura necesario para construir la pantalla mensual.
 *
 * Agrupar estas lecturas evita repetir el costo HTTP/JWT para agentes,
 * turnos, secuencias y excepciones.
 */
public record ProgramacionContextoResponse(
        List<TrabajadorPublicResponse> agentes,
        List<ProgramacionDiaResponse> turnos,
        List<SecuenciaAgenteResponse> secuencias,
        List<AgenteProgramacionExcepcionResponse> excepciones
) {
}
