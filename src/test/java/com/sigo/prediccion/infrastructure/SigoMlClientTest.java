package com.sigo.prediccion.infrastructure;

import com.sigo.prediccion.api.dto.PrediccionAsistenciaRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SigoMlClientTest {
    private final PrediccionAsistenciaRequest request =
            new PrediccionAsistenciaRequest(3L, 2L, LocalDate.of(2026, 11, 5), 18);

    @Test
    void deshabilitadoNoIntentaRealizarSolicitudes() {
        SigoMlClient client = new SigoMlClient(false, "http://127.0.0.1:8000", "");
        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> client.predecir(request));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    @Test
    void habilitadoSinCredencialNoRealizaSolicitudes() {
        SigoMlClient client = new SigoMlClient(true, "http://127.0.0.1:8000", "");
        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> client.predecir(request));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }
}
