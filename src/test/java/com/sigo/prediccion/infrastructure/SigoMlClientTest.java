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
    @Test
    void enviaFechaIsoYCamposCamelCaseAFastApi() throws Exception {
        com.sun.net.httpserver.HttpServer server =
                com.sun.net.httpserver.HttpServer.create(
                        new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        java.util.concurrent.atomic.AtomicReference<String> bodyRecibido =
                new java.util.concurrent.atomic.AtomicReference<>();
        server.createContext("/internal/v1/asistencia/predecir", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8);
            bodyRecibido.set(body);
            String response = """
                    {"plazaId":3,"turnoId":2,"fecha":"2026-11-05","programados":18,
                     "ausenciasEstimadas":1,"coberturaEstimada":17,
                     "modelo":"baseline","version":"0.1.0","origenDatos":"real",
                     "advertencia":null}
                    """;
            byte[] bytes = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (java.io.OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
        server.start();
        try {
            SigoMlClient client = new SigoMlClient(
                    true, "http://127.0.0.1:" + server.getAddress().getPort(),
                    "token-prueba");
            var resultado = client.predecir(request);
            assertEquals(1, resultado.ausenciasEstimadas());
            assertEquals(17, resultado.coberturaEstimada());
            assertNotNull(bodyRecibido.get());
            var json = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(bodyRecibido.get());
            assertEquals("2026-11-05", json.get("fecha").asText());
            assertTrue(json.get("fecha").isTextual());
            assertEquals(3, json.get("plazaId").asInt());
            assertEquals(2, json.get("turnoId").asInt());
            assertEquals(18, json.get("programados").asInt());
        } finally {
            server.stop(0);
        }
    }

}
