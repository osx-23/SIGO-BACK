package com.sigo.prediccion.infrastructure;

import com.sigo.prediccion.application.port.out.PrediccionAsistenciaPort;
import com.sigo.prediccion.domain.PrediccionAsistenciaParametros;
import com.sigo.prediccion.domain.PrediccionAsistenciaResultado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

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
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Las predicciones de SIGO-ML no estan habilitadas");
        }
        if (internalToken == null || internalToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se ha configurado la credencial interna de SIGO-ML");
        }
        try {
            return restClient.post()
                    .uri("/internal/v1/asistencia/predecir")
                    .header("X-Internal-Token", internalToken)
                    .body(request)
                    .retrieve()
                    .body(PrediccionAsistenciaResultado.class);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El servicio de predicciones no esta disponible temporalmente", e);
        }
    }
}
