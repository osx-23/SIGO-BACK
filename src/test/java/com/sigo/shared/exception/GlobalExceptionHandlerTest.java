package com.sigo.shared.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    @Test
    void bloqueoDevuelve429TiempoEnSegundosYRetryAfter() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        var respuesta = handler.tooMany(
                new TooManyRequestsException("Tu cuenta ha sido bloqueada temporalmente.", 900)
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, respuesta.getStatusCode());
        assertEquals("900", respuesta.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        assertEquals(900L, respuesta.getBody().get("retryAfterSeconds"));
        assertEquals("Tu cuenta ha sido bloqueada temporalmente.", respuesta.getBody().get("message"));
    }
}
