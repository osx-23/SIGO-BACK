package com.sigo.chat.domain;

public record ChatTurn(
        String role,
        String text
) {
}
