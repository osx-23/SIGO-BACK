package com.sigo.chat.service;

import com.sigo.chat.dto.ChatRequest;
import com.sigo.chat.dto.SigoChatPlan;
import com.sigo.chat.dto.SigoToolRequest;
import com.sigo.chat.dto.SigoToolResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final GeminiService geminiService;
    private final SigoReadOnlyQueryService queryService;
    private final ChatResponseFormatter formatter;

    public String procesar(String mensaje, List<ChatRequest.ChatTurn> historial) {
        String contexto = construirContexto(historial);

        try {
            SigoChatPlan plan = geminiService.interpretar(mensaje, contexto);
            List<SigoToolRequest> herramientas = plan.toolsSeguras();

            if (herramientas.isEmpty()) {
                return "No encontré una consulta concreta que pueda resolver con la información disponible en SIGO. Puedes preguntarme, por ejemplo, por asistencia, ausencias, relevos o vías.";
            }

            List<SigoToolResult> resultados = new ArrayList<>();
            for (SigoToolRequest herramienta : herramientas) {
                resultados.add(queryService.ejecutar(herramienta));
            }

            String datos = formatter.formatear(resultados);
            if (datos == null || datos.isBlank()) {
                return "No encontré información registrada en SIGO para responder esa consulta.";
            }

            try {
                return geminiService.responderNatural(mensaje, contexto, datos);
            } catch (Exception naturalError) {
                System.err.println("No se pudo naturalizar la respuesta del chat: " + naturalError.getMessage());
                return datos;
            }
        } catch (Exception e) {
            System.err.println("Error en Asistente SIGO: " + e.getMessage());
            e.printStackTrace();

            if (e.getMessage() != null && e.getMessage().contains("temporalmente")) {
                return e.getMessage();
            }

            return "No pude procesar la consulta en este momento. Intenta nuevamente.";
        }
    }

    private String construirContexto(List<ChatRequest.ChatTurn> historial) {
        if (historial == null || historial.isEmpty()) return "Sin conversación previa.";

        StringBuilder out = new StringBuilder();
        for (ChatRequest.ChatTurn turno : historial) {
            if (turno == null || turno.text() == null || turno.text().isBlank()) continue;
            String rol = "assistant".equalsIgnoreCase(turno.role()) ? "Asistente" : "Usuario";
            String texto = turno.text().trim();
            if (texto.length() > 700) texto = texto.substring(0, 700);
            out.append(rol).append(": ").append(texto).append('\n');
        }
        return out.isEmpty() ? "Sin conversación previa." : out.toString().trim();
    }
}
