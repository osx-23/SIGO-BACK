package com.sigo.chat.api.controller;

import com.sigo.chat.api.dto.ChatRequest;
import com.sigo.chat.api.dto.ChatResponse;
import com.sigo.chat.application.port.in.ChatUseCase;
import com.sigo.chat.domain.ChatTurn;
import com.sigo.security.application.port.in.UsuarioActualUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatUseCase chatUseCase;
    private final UsuarioActualUseCase usuarioActualUseCase;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @Valid
            @RequestBody ChatRequest request
    ) {
        UsuarioActualUseCase.UsuarioActual usuario =
                usuarioActualUseCase.requireActual();

        if (!"SUPERVISOR".equals(usuario.rol())
                && !"CONTROLADOR".equals(usuario.rol())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El Asistente SIGO está disponible solo para supervisores y controladores"
            );
        }

        List<ChatTurn> historial = request
                .historySegura()
                .stream()
                .map(turno ->
                        new ChatTurn(
                                turno.role(),
                                turno.text()
                        )
                )
                .toList();

        return ResponseEntity.ok(
                new ChatResponse(
                        chatUseCase.procesar(
                                request.message(),
                                historial
                        )
                )
        );
    }
}
