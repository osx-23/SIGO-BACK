package com.sigo.prediccion.application;

import com.sigo.prediccion.application.port.out.PrediccionAsistenciaPort;
import com.sigo.prediccion.domain.PrediccionAsistenciaParametros;
import com.sigo.prediccion.domain.PrediccionAsistenciaResultado;
import lombok.RequiredArgsConstructor;
import com.sigo.prediccion.domain.PrediccionInvalidaException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrediccionAsistenciaService {
    private final PrediccionAsistenciaPort mlPort;

    public PrediccionAsistenciaResultado predecir(PrediccionAsistenciaParametros request) {
        PrediccionAsistenciaResultado result = mlPort.predecir(request);
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
            throw new PrediccionInvalidaException("SIGO-ML devolvio una prediccion no valida");
        }
        return result;
    }
}
