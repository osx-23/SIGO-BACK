package com.sigo.prediccion.application;

import com.sigo.prediccion.application.port.out.PrediccionAsistenciaPort;
import com.sigo.prediccion.domain.PrediccionAsistenciaParametros;
import com.sigo.prediccion.domain.PrediccionAsistenciaResultado;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrediccionAsistenciaServiceTest {
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 5);

    private PrediccionAsistenciaParametros request() {
        return new PrediccionAsistenciaParametros(3L, 2L, FECHA, 18);
    }

    private PrediccionAsistenciaResultado result(int ausencias, int cobertura) {
        return new PrediccionAsistenciaResultado(3L, 2L, FECHA, 18,
                ausencias, cobertura, "baseline", "0.1.0", "real", null);
    }

    @Test
    void devuelvePrediccionValidaDeSigoMl() {
        PrediccionAsistenciaPort port = mock(PrediccionAsistenciaPort.class);
        when(port.predecir(request())).thenReturn(result(1, 17));
        assertEquals(17, new PrediccionAsistenciaService(port)
                .predecir(request()).coberturaEstimada());
    }

    @Test
    void rechazaCifrasInconsistentesDeModeloExterno() {
        PrediccionAsistenciaPort port = mock(PrediccionAsistenciaPort.class);
        when(port.predecir(request())).thenReturn(result(3, 18));
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> new PrediccionAsistenciaService(port).predecir(request()));
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
    }

    @Test
    void rechazaAusenciasMayoresQueProgramados() {
        PrediccionAsistenciaPort port = mock(PrediccionAsistenciaPort.class);
        when(port.predecir(request())).thenReturn(result(20, -2));
        assertThrows(ResponseStatusException.class,
                () -> new PrediccionAsistenciaService(port).predecir(request()));
    }
}
