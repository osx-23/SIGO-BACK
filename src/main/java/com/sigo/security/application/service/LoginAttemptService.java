package com.sigo.security.application.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_INTENTOS = 5;
    private static final Duration VENTANA = Duration.ofMinutes(10);
    private static final Duration BLOQUEO = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, EstadoIntentos> intentos = new ConcurrentHashMap<>();

    public void verificar(Integer codigo, String ip) {
        EstadoIntentos estado = intentos.get(clave(codigo, ip));
        if (estado == null) return;

        Instant ahora = Instant.now();
        synchronized (estado) {
            if (estado.bloqueadoHasta != null && ahora.isBefore(estado.bloqueadoHasta)) {
                long segundos = Duration.between(ahora, estado.bloqueadoHasta).toSeconds();
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Demasiados intentos fallidos. Intenta nuevamente en " + Math.max(1, segundos / 60) + " minuto(s)."
                );
            }

            if (estado.primero != null && Duration.between(estado.primero, ahora).compareTo(VENTANA) > 0) {
                intentos.remove(clave(codigo, ip), estado);
            }
        }
    }

    public void registrarFallo(Integer codigo, String ip) {
        String clave = clave(codigo, ip);
        Instant ahora = Instant.now();
        EstadoIntentos estado = intentos.computeIfAbsent(clave, k -> new EstadoIntentos());

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
        intentos.remove(clave(codigo, ip));
    }

    private String clave(Integer codigo, String ip) {
        return String.valueOf(codigo) + "|" + (ip == null ? "unknown" : ip);
    }

    private static final class EstadoIntentos {
        private int fallos;
        private Instant primero;
        private Instant bloqueadoHasta;
    }
}
