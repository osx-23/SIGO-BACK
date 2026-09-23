package com.sigo.chat.application.port.out;

import com.sigo.chat.domain.SigoChatPlan;

public interface ChatAiPort {

    SigoChatPlan interpretar(
            String pregunta,
            String contextoConversacion
    );

    String responderNatural(
            String pregunta,
            String contextoConversacion,
            String datosSigo
    );
}
