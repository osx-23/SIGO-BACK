package com.sigo.chat.application.service;

import com.sigo.chat.application.port.in.ChatUseCase;
import com.sigo.chat.application.port.out.ChatAiPort;
import com.sigo.chat.application.port.out.SigoQueryPort;
import com.sigo.chat.domain.ChatTurn;
import com.sigo.chat.domain.SigoChatPlan;
import com.sigo.chat.domain.SigoToolRequest;
import com.sigo.chat.domain.SigoToolResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService implements ChatUseCase {

    private final ChatAiPort aiPort;
    private final SigoQueryPort queryPort;
    private final ChatResponseFormatter formatter;

    @Override
    public String procesar(
            String mensaje,
            List<ChatTurn> historial
    ) {
        String contexto =
                construirContexto(historial);

        try {
            SigoChatPlan plan =
                    aiPort.interpretar(
                            mensaje,
                            contexto
                    );

            List<SigoToolRequest> herramientas =
                    plan.toolsSeguras();

            if (herramientas.isEmpty()) {
                return "No encontré una consulta concreta que pueda resolver con la información disponible en SIGO. Puedes preguntarme, por ejemplo, por asistencia, ausencias, relevos o vías.";
            }

            List<SigoToolResult> resultados =
                    new ArrayList<>();

            for (SigoToolRequest herramienta
                    : herramientas) {
                resultados.add(
                        queryPort.ejecutar(
                                herramienta
                        )
                );
            }

            String datos =
                    formatter.formatear(
                            resultados
                    );

            if (datos == null || datos.isBlank()) {
                return "No encontré información registrada en SIGO para responder esa consulta.";
            }

            try {
                return aiPort.responderNatural(
                        mensaje,
                        contexto,
                        datos
                );
            } catch (Exception naturalError) {
                System.err.println(
                        "No se pudo naturalizar la respuesta del chat: "
                                + naturalError.getMessage()
                );
                return datos;
            }

        } catch (Exception exception) {
            System.err.println(
                    "Error en Asistente SIGO: "
                            + exception.getMessage()
            );

            exception.printStackTrace();

            if (exception.getMessage() != null
                    && exception.getMessage()
                    .contains("temporalmente")) {
                return exception.getMessage();
            }

            return "No pude procesar la consulta en este momento. Intenta nuevamente.";
        }
    }

    private String construirContexto(
            List<ChatTurn> historial
    ) {
        if (historial == null || historial.isEmpty()) {
            return "Sin conversación previa.";
        }

        StringBuilder out =
                new StringBuilder();

        for (ChatTurn turno : historial) {
            if (turno == null
                    || turno.text() == null
                    || turno.text().isBlank()) {
                continue;
            }

            String rol =
                    "assistant".equalsIgnoreCase(
                            turno.role()
                    )
                            ? "Asistente"
                            : "Usuario";

            String texto = turno.text().trim();

            if (texto.length() > 700) {
                texto = texto.substring(0, 700);
            }

            out.append(rol)
                    .append(": ")
                    .append(texto)
                    .append('\n');
        }

        return out.isEmpty()
                ? "Sin conversación previa."
                : out.toString().trim();
    }
}
