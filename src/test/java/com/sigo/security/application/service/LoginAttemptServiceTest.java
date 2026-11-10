package com.sigo.security.application.service;

import com.sigo.shared.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTest {

    @Test
    void bloqueaEnElDecimoIntentoFallido() {
        LoginAttemptService service = new LoginAttemptService();
        Integer codigo = 287;

        for (int i = 0; i < 9; i++) {
            service.registrarFallo(codigo, "127.0.0.1");
            assertDoesNotThrow(() -> service.verificar(codigo, "127.0.0.1"));
        }

        service.registrarFallo(codigo, "127.0.0.1");
        TooManyRequestsException bloqueo = assertThrows(
                TooManyRequestsException.class, () -> service.verificar(codigo, "127.0.0.1"));
        assertTrue(bloqueo.retryAfterSeconds() > 0);
        assertTrue(bloqueo.retryAfterSeconds() <= 900);
        assertTrue(bloqueo.getMessage().contains("Tu cuenta ha sido bloqueada"));
        assertTrue(bloqueo.getMessage().contains("Debes esperar"));
        assertThrows(TooManyRequestsException.class, () -> service.verificar(codigo, "127.0.0.1"));
    }

    @Test
    void contabilizaIntentosPorCodigoSinAfectarAOtrosTrabajadores() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < 10; i++) {
            service.registrarFallo(287, "127.0.0.1");
        }

        assertThrows(TooManyRequestsException.class, () -> service.verificar(287, "127.0.0.1"));
        assertDoesNotThrow(() -> service.verificar(550, "127.0.0.1"));
    }

    @Test
    void limpiaLosIntentosTrasUnIngresoCorrecto() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < 9; i++) {
            service.registrarFallo(287, "127.0.0.1");
        }

        service.limpiar(287, "127.0.0.1");
        service.registrarFallo(287, "127.0.0.1");
        assertDoesNotThrow(() -> service.verificar(287, "127.0.0.1"));
    }
    @Test
    void noBloqueaElMismoCodigoDesdeOtraDireccion() {
        LoginAttemptService service = new LoginAttemptService();
        for (int i = 0; i < 10; i++) service.registrarFallo(287, "192.0.2.1");
        assertThrows(TooManyRequestsException.class, () -> service.verificar(287, "192.0.2.1"));
        assertDoesNotThrow(() -> service.verificar(287, "192.0.2.2"));
    }
}
