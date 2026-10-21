package com.sigo.prediccion.application;

import com.sigo.prediccion.api.dto.PrediccionAsistenciaRequest;
import com.sigo.prediccion.api.dto.PrediccionAsistenciaResponse;
import com.sigo.prediccion.infrastructure.SigoMlClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PrediccionAsistenciaService {
    private final SigoMlClient mlClient;

    public PrediccionAsistenciaResponse predecir(PrediccionAsistenciaRequest request) {
        PrediccionAsistenciaResponse result = mlClient.predecir(request);
        // Nunca publicar una prediccion operativa si el servicio externo devuelve
        // una respuesta incompleta o incompatible con el numero de programados.
        if (result == null
                || result.ausenciasEstimadas() == null
                || result.coberturaEstimada() == null
                || result.programados() == null
                || !request.plazaId().equals(result.plazaId())
                || !request.turnoId().equals(result.turnoId())
                || !request.fecha().equals(result.fecha())
                || !request.programados().equals(result.programados())
                || result.ausenciasEstimadas() < 0
                || result.ausenciasEstimadas() > request.programados()
                || result.coberturaEstimada() != request.programados() - result.ausenciasEstimadas()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "SIGO-ML devolvio una prediccion no valida"
            );
        }
        return result;
    }
}
