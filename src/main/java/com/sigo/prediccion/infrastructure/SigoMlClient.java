package com.sigo.prediccion.infrastructure;

import com.sigo.prediccion.application.port.out.PrediccionAsistenciaPort;
import com.sigo.prediccion.domain.PrediccionAsistenciaParametros;
import com.sigo.prediccion.domain.PrediccionAsistenciaResultado;
import org.springframework.beans.factory.annotation.Value;
import com.sigo.prediccion.domain.PrediccionNoDisponibleException;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.util.Map;

@Component
public class SigoMlClient implements PrediccionAsistenciaPort {
    private final RestClient restClient;
    private final boolean enabled;
    private final String internalToken;

    public SigoMlClient(
            @Value("${sigo.ml.enabled:false}") boolean enabled,
            @Value("${sigo.ml.url:http://localhost:8000}") String baseUrl,
            @Value("${sigo.ml.internal-token:}") String internalToken
    ) {
        this.enabled = enabled;
        this.internalToken = internalToken;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    @Override
    public PrediccionAsistenciaResultado predecir(PrediccionAsistenciaParametros request) {
        if (!enabled) {
            throw new PrediccionNoDisponibleException("Las predicciones de SIGO-ML no estan habilitadas");
        }
        if (internalToken == null || internalToken.isBlank()) {
            throw new PrediccionNoDisponibleException("No se ha configurado la credencial interna de SIGO-ML");
        }
        try {
            return restClient.post()
                    .uri("/internal/v1/asistencia/predecir")
                    .header("X-Internal-Token", internalToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "plazaId", request.plazaId(),
                            "turnoId", request.turnoId(),
                            "fecha", request.fecha().toString(),
                            "programados", request.programados()
                    ))
                    .retrieve()
                    .body(PrediccionAsistenciaResultado.class);
        } catch (RestClientException e) {
            throw new PrediccionNoDisponibleException("El servicio de predicciones no esta disponible temporalmente", e);
        }
    }
}
