package com.sigo.security.application.service;

import com.sigo.shared.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_INTENTOS = 10;
    private static final Duration VENTANA = Duration.ofMinutes(10);
    private static final Duration BLOQUEO = Duration.ofMinutes(15);

    private final ConcurrentHashMap<Integer, EstadoIntentos> intentos = new ConcurrentHashMap<>();

    public void verificar(Integer codigo) {
        EstadoIntentos estado = intentos.get(codigo);
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
                intentos.remove(codigo, estado);
            }
        }
    }

    public void registrarFallo(Integer codigo) {
        Instant ahora = Instant.now();
        EstadoIntentos estado = intentos.computeIfAbsent(codigo, k -> new EstadoIntentos());

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

    public void limpiar(Integer codigo) {
        intentos.remove(codigo);
    }

    private static final class EstadoIntentos {
        private int fallos;
        private Instant primero;
        private Instant bloqueadoHasta;
    }
}
