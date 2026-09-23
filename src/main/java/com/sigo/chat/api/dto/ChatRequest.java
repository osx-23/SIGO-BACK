package com.sigo.chat.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ChatRequest(
        @NotBlank String message,
        List<ChatTurn> history
) {
    public List<ChatTurn> historySegura() {
        if (history == null || history.isEmpty()) return List.of();
        int inicio = Math.max(0, history.size() - 6);
        return history.subList(inicio, history.size()).stream()
                .filter(t -> t != null && t.text() != null && !t.text().isBlank())
                .toList();
    }

    public record ChatTurn(String role, String text) {}
}
