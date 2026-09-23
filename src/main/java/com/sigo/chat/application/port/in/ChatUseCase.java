package com.sigo.chat.application.port.in;

import com.sigo.chat.domain.ChatTurn;

import java.util.List;

public interface ChatUseCase {

    String procesar(
            String mensaje,
            List<ChatTurn> historial
    );
}
