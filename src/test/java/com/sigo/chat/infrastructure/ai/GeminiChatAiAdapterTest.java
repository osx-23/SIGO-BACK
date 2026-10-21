package com.sigo.chat.infrastructure.ai;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeminiChatAiAdapterTest {
    @Test
    void diaActualSeCalculaEnLimaYNoEnUtc() {
        // 00:30 UTC del 21 de octubre sigue siendo 20 de octubre en Lima.
        Instant horaUtc = Instant.parse("2026-10-21T00:30:00Z");
        assertEquals(
                LocalDate.of(2026, 10, 20),
                GeminiChatAiAdapter.fechaEnLima(horaUtc)
        );
    }
}
