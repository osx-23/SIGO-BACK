package com.sigo.prediccion.application;

import com.sigo.prediccion.api.dto.PrediccionAsistenciaRequest;
import com.sigo.prediccion.api.dto.PrediccionAsistenciaResponse;
import com.sigo.prediccion.infrastructure.SigoMlClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrediccionAsistenciaServiceTest {
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 5);

    private PrediccionAsistenciaRequest request() {
        return new PrediccionAsistenciaRequest(3L, 2L, FECHA, 18);
    }

    private PrediccionAsistenciaResponse result(int ausencias, int cobertura) {
        return new PrediccionAsistenciaResponse(3L, 2L, FECHA, 18,
                ausencias, cobertura, "baseline", "0.1.0", "real", null);
    }

    @Test
    void devuelvePrediccionValidaDeSigoMl() {
        SigoMlClient client = mock(SigoMlClient.class);
        when(client.predecir(request())).thenReturn(result(1, 17));
        assertEquals(17, new PrediccionAsistenciaService(client)
                .predecir(request()).coberturaEstimada());
    }

    @Test
    void rechazaCifrasInconsistentesDeModeloExterno() {
        SigoMlClient client = mock(SigoMlClient.class);
        when(client.predecir(request())).thenReturn(result(3, 18));
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> new PrediccionAsistenciaService(client).predecir(request()));
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
    }

    @Test
    void rechazaAusenciasMayoresQueProgramados() {
        SigoMlClient client = mock(SigoMlClient.class);
        when(client.predecir(request())).thenReturn(result(20, -2));
        assertThrows(ResponseStatusException.class,
                () -> new PrediccionAsistenciaService(client).predecir(request()));
    }
}
