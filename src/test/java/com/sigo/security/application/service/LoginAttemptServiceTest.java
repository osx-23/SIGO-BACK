package com.sigo.security.application.service;

import com.sigo.shared.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginAttemptServiceTest {

    @Test
    void bloqueaEnElDecimoIntentoFallido() {
        LoginAttemptService service = new LoginAttemptService();
        Integer codigo = 287;

        for (int i = 0; i < 9; i++) {
            service.registrarFallo(codigo);
            assertDoesNotThrow(() -> service.verificar(codigo));
        }

        service.registrarFallo(codigo);
        assertThrows(TooManyRequestsException.class, () -> service.verificar(codigo));
        assertThrows(TooManyRequestsException.class, () -> service.verificar(codigo));
    }

    @Test
    void contabilizaIntentosPorCodigoSinAfectarAOtrosTrabajadores() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < 10; i++) {
            service.registrarFallo(287);
        }

        assertThrows(TooManyRequestsException.class, () -> service.verificar(287));
        assertDoesNotThrow(() -> service.verificar(550));
    }

    @Test
    void limpiaLosIntentosTrasUnIngresoCorrecto() {
        LoginAttemptService service = new LoginAttemptService();

        for (int i = 0; i < 9; i++) {
            service.registrarFallo(287);
        }

        service.limpiar(287);
        service.registrarFallo(287);
        assertDoesNotThrow(() -> service.verificar(287));
    }
}
