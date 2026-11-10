package com.sigo.security.application.service;

import com.sigo.shared.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Service
public class LoginAttemptService {

    private static final int MAX_INTENTOS = 10;
    private static final Duration VENTANA = Duration.ofMinutes(10);
    private static final Duration BLOQUEO = Duration.ofMinutes(15);

    private final Cache<String, EstadoIntentos> intentos = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();

    public void verificar(Integer codigo, String ip) {
        String clave = clave(codigo, ip);
        EstadoIntentos estado = intentos.getIfPresent(clave);
        if (estado == null) return;

        Instant ahora = Instant.now();
        synchronized (estado) {
            if (estado.bloqueadoHasta != null && ahora.isBefore(estado.bloqueadoHasta)) {
                long milisegundos = Duration.between(ahora, estado.bloqueadoHasta).toMillis();
                long segundos = Math.max(1, (milisegundos + 999) / 1000);
                long minutos = (segundos + 59) / 60;
                throw new TooManyRequestsException(
                        "Tu cuenta ha sido bloqueada temporalmente por demasiados intentos fallidos. "
                                + "Debes esperar " + minutos
                                + (minutos == 1 ? " minuto." : " minutos."),
                        segundos
                );
            }

            if (estado.primero != null && Duration.between(estado.primero, ahora).compareTo(VENTANA) > 0) {
                intentos.asMap().remove(clave, estado);
            }
        }
    }

    public void registrarFallo(Integer codigo, String ip) {
        String clave = clave(codigo, ip);
        Instant ahora = Instant.now();
        EstadoIntentos estado = intentos.get(clave, k -> new EstadoIntentos());

        synchronized (estado) {
            if (estado.primero == null || Duration.between(estado.primero, ahora).compareTo(VENTANA) > 0) {
                estado.primero = ahora;
                estado.fallos = 0;
                estado.bloqueadoHasta = null;
            }

            estado.fallos++;
            if (estado.fallos >= MAX_INTENTOS) {
                estado.bloqueadoHasta = ahora.plus(BLOQUEO);
            }
        }
    }

    public void limpiar(Integer codigo, String ip) {
        intentos.invalidate(clave(codigo, ip));
    }

    private String clave(Integer codigo, String ip) {
        return codigo + "|" + (ip == null ? "unknown" : ip);
    }

    private static final class EstadoIntentos {
        private int fallos;
        private Instant primero;
        private Instant bloqueadoHasta;
    }
}
