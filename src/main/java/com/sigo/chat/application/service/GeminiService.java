package com.sigo.chat.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigo.chat.api.dto.SigoChatPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.6-flash}")
    private String model;

    public SigoChatPlan interpretar(String pregunta, String contextoConversacion) {
        exigirApiKey();
        String prompt = construirPrompt(pregunta, contextoConversacion);

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "temperature", 0,
                        "responseMimeType", "application/json"
                )
        );

        String responseBody = ejecutarConReintentos(url(), body);

        try {
            String json = extraerTexto(responseBody);
            if (json == null || json.isBlank()) {
                throw new IllegalStateException("Gemini devolvió una respuesta vacía.");
            }
            return objectMapper.readValue(json, SigoChatPlan.class);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo interpretar la respuesta de Gemini.", e);
        }
    }

    public String responderNatural(String pregunta, String contextoConversacion, String datosSigo) {
        exigirApiKey();

        String prompt = """
                Eres el Asistente SIGO de Lima Expresa.
                Responde en español natural, claro y profesional, como un asistente conversacional útil.

                REGLAS OBLIGATORIAS:
                - Responde exclusivamente con la información entregada en DATOS SIGO.
                - El HISTORIAL solo sirve para entender referencias como "y ayer", "esa plaza" o "ese trabajador"; nunca lo uses como fuente de datos actuales si DATOS SIGO no lo confirma.
                - No inventes datos, causas, nombres, fechas ni conclusiones que no estén sustentadas.
                - Si falta información para responder algo, dilo de forma natural.
                - Evita formatos técnicos como "Sección:", pipes (|), nombres de campos de base de datos o listados mecánicos.
                - No repitas todos los datos si no son necesarios para responder la pregunta.
                - Prioriza una respuesta directa en el primer enunciado y luego agrega contexto útil.
                - Usa porcentajes y cantidades de manera natural.
                - Si hay varios indicadores, intégralos en frases fáciles de leer.
                - Puedes usar viñetas solo cuando realmente ayuden a comparar varios elementos.
                - No uses tablas.
                - Mantén la respuesta normalmente entre 2 y 6 oraciones.
                - Si la pregunta es una continuación, responde como continuación, sin reiniciar la conversación.

                Ejemplo de estilo:
                En septiembre, la asistencia fue de 91.73%%: se registraron 122 presentes de 133 programados. Además, hubo 11 ausencias y se solicitó apoyo en 3 ocasiones. En ese mismo periodo se registraron 4 relevos, sin elementos ni vías observadas.

                HISTORIAL RECIENTE:
                %s

                PREGUNTA ACTUAL:
                %s

                DATOS SIGO:
                %s
                """.formatted(contextoConversacion, pregunta, datosSigo);

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.25,
                        "maxOutputTokens", 500
                )
        );

        String responseBody = ejecutarConReintentos(url(), body);
        try {
            String texto = extraerTexto(responseBody);
            if (texto == null || texto.isBlank()) {
                throw new IllegalStateException("Gemini devolvió una respuesta vacía.");
            }
            return texto.trim();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo redactar la respuesta del asistente.", e);
        }
    }

    private void exigirApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY no está configurada.");
        }
    }

    private String url() {
        return "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;
    }

    private String extraerTexto(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        return root.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText();
    }

    private String ejecutarConReintentos(String url, Map<String, Object> body) {
        int maxIntentos = 3;
        for (int intento = 1; intento <= maxIntentos; intento++) {
            try {
                return restClient.post().uri(url).body(body).retrieve().body(String.class);
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                boolean temporal = status == 429 || status == 503;
                if (temporal && intento < maxIntentos) {
                    dormir(700L * intento);
                    continue;
                }
                if (temporal) {
                    throw new IllegalStateException("El asistente de IA está temporalmente ocupado. Intenta nuevamente en unos segundos.");
                }
                throw new IllegalStateException("Error de Gemini: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            }
        }
        throw new IllegalStateException("No se pudo obtener respuesta de Gemini.");
    }

    private void dormir(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La consulta fue interrumpida.");
        }
    }

    private String construirPrompt(String pregunta, String contextoConversacion) {
        LocalDate hoy = LocalDate.now();

        return """
                Eres el intérprete de consultas del sistema SIGO.
                NO respondas la pregunta del usuario.
                Tu única tarea es devolver JSON válido indicando qué herramientas de SOLO LECTURA necesita ejecutar el backend.

                Fecha actual: %s

                CONTEXTO DE CONVERSACIÓN:
                %s

                Usa el contexto únicamente para resolver referencias de la pregunta actual como "y ayer", "esa plaza", "ese trabajador" o "lo mismo".
                La herramienta debe corresponder siempre a la PREGUNTA ACTUAL.

                Áreas disponibles:
                - Trabajadores
                - Asistencia
                - Ausencias
                - Apoyo solicitado
                - Plazas
                - Turnos
                - Relevos
                - Vías
                - Motivos
                - Evidencias de asistencia
                - Evidencias de relevos

                Incidencias todavía NO está implementado en el backend actual.
                Si preguntan por incidencias usa tool = INCIDENCIAS.

                Herramientas permitidas:
                BUSCAR_TRABAJADOR
                RESUMEN_ASISTENCIA
                AUSENCIAS
                APOYO_SOLICITADO
                LISTAR_PLAZAS
                LISTAR_TURNOS
                LISTAR_MOTIVOS
                RELEVOS
                VIAS
                EVIDENCIAS_ASISTENCIA
                EVIDENCIAS_RELEVO
                RESUMEN_GENERAL
                INCIDENCIAS
                DESCONOCIDO

                Campos disponibles:
                tool, codigo, trabajador, plaza, turno, motivo, estado,
                relevoId, asistenciaId, desde, hasta, limite.

                Devuelve exactamente:
                {
                  "tools": [
                    {
                      "tool": "NOMBRE_HERRAMIENTA",
                      "codigo": null,
                      "trabajador": null,
                      "plaza": null,
                      "turno": null,
                      "motivo": null,
                      "estado": null,
                      "relevoId": null,
                      "asistenciaId": null,
                      "desde": null,
                      "hasta": null,
                      "limite": 20
                    }
                  ]
                }

                Reglas:
                - Fechas en YYYY-MM-DD.
                - "hoy" = fecha actual.
                - "ayer" = fecha actual menos un día.
                - "este mes" = primer y último día del mes actual.
                - Para información general de una plaza o periodo usa RESUMEN_GENERAL.
                - Una pregunta puede requerir varias herramientas.
                - Nunca generes SQL.
                - Nunca inventes códigos, plazas, fechas o IDs.
                - Para faltas, cuándo faltó, por qué faltó o ausencias usa AUSENCIAS.
                - Para porcentaje, programados, presentes u operativos usa RESUMEN_ASISTENCIA.
                - Para apoyo usa APOYO_SOLICITADO.
                - Para vías usa VIAS.
                - Para relevos/checklist/elementos observados usa RELEVOS.
                - Para fotos usa EVIDENCIAS_ASISTENCIA o EVIDENCIAS_RELEVO.
                - limite máximo 50.

                PREGUNTA ACTUAL:
                %s
                """.formatted(hoy, contextoConversacion, pregunta);
    }
}
