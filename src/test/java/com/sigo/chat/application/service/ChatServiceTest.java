package com.sigo.chat.application.service;

import com.sigo.chat.application.port.out.ChatAiPort;
import com.sigo.chat.application.port.out.SigoQueryPort;
import com.sigo.chat.domain.ChatTurn;
import com.sigo.chat.domain.SigoChatPlan;
import com.sigo.chat.domain.SigoTool;
import com.sigo.chat.domain.SigoToolRequest;
import com.sigo.chat.domain.SigoToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatServiceTest {

    @Test
    void ejecutaHerramientaYNaturalizaRespuesta() {
        ChatService service =
                new ChatService(
                        new StubAiPort(false),
                        new StubQueryPort(),
                        new ChatResponseFormatter()
                );

        String respuesta = service.procesar(
                "¿Cómo estuvo la asistencia?",
                List.of(
                        new ChatTurn(
                                "user",
                                "Hablemos de P4"
                        )
                )
        );

        assertEquals(
                "La asistencia fue de 90%.",
                respuesta
        );
    }

    @Test
    void usaDatosFormateadosSiFallaNaturalizacion() {
        ChatService service =
                new ChatService(
                        new StubAiPort(true),
                        new StubQueryPort(),
                        new ChatResponseFormatter()
                );

        String respuesta = service.procesar(
                "Asistencia",
                List.of()
        );

        assertTrue(
                respuesta.contains("% asistencia: 90")
        );
    }

    @Test
    void respondeCuandoNoHayHerramientas() {
        ChatService service =
                new ChatService(
                        new ChatAiPort() {
                            @Override
                            public SigoChatPlan interpretar(
                                    String pregunta,
                                    String contextoConversacion
                            ) {
                                return new SigoChatPlan(
                                        List.of()
                                );
                            }

                            @Override
                            public String responderNatural(
                                    String pregunta,
                                    String contextoConversacion,
                                    String datosSigo
                            ) {
                                return "";
                            }
                        },
                        request -> null,
                        new ChatResponseFormatter()
                );

        String respuesta =
                service.procesar(
                        "Hola",
                        List.of()
                );

        assertTrue(
                respuesta.contains(
                        "No encontré una consulta concreta"
                )
        );
    }

    private static class StubAiPort
            implements ChatAiPort {

        private final boolean fallarNaturalizacion;

        private StubAiPort(
                boolean fallarNaturalizacion
        ) {
            this.fallarNaturalizacion =
                    fallarNaturalizacion;
        }

        @Override
        public SigoChatPlan interpretar(
                String pregunta,
                String contextoConversacion
        ) {
            return new SigoChatPlan(
                    List.of(
                            new SigoToolRequest(
                                    SigoTool.RESUMEN_ASISTENCIA,
                                    null,
                                    null,
                                    "P4",
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    20
                            )
                    )
            );
        }

        @Override
        public String responderNatural(
                String pregunta,
                String contextoConversacion,
                String datosSigo
        ) {
            if (fallarNaturalizacion) {
                throw new IllegalStateException(
                        "Gemini no disponible"
                );
            }

            return "La asistencia fue de 90%.";
        }
    }

    private static class StubQueryPort
            implements SigoQueryPort {

        @Override
        public SigoToolResult ejecutar(
                SigoToolRequest request
        ) {
            return new SigoToolResult(
                    "Resumen de asistencia",
                    List.of(
                            Map.of(
                                    "porcentaje_asistencia",
                                    90
                            )
                    ),
                    "P4"
            );
        }
    }
}
