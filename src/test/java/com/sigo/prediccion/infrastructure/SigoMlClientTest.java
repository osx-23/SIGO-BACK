package com.sigo.prediccion.infrastructure;

import com.sigo.prediccion.domain.PrediccionAsistenciaParametros;
import org.junit.jupiter.api.Test;
import com.sigo.prediccion.domain.PrediccionNoDisponibleException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SigoMlClientTest {
    private final PrediccionAsistenciaParametros request =
            new PrediccionAsistenciaParametros(3L, 2L, LocalDate.of(2026, 11, 5), 18);

    @Test
    void deshabilitadoNoIntentaRealizarSolicitudes() {
        SigoMlClient client = new SigoMlClient(false, "http://127.0.0.1:8000", "");
        PrediccionNoDisponibleException exception =
                assertThrows(PrediccionNoDisponibleException.class, () -> client.predecir(request));
        assertNotNull(exception.getMessage());
    }

    @Test
    void habilitadoSinCredencialNoRealizaSolicitudes() {
        SigoMlClient client = new SigoMlClient(true, "http://127.0.0.1:8000", "");
        PrediccionNoDisponibleException exception =
                assertThrows(PrediccionNoDisponibleException.class, () -> client.predecir(request));
        assertNotNull(exception.getMessage());
    }
}
