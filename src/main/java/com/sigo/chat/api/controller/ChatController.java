package com.sigo.chat.api.controller;

import com.sigo.chat.api.dto.ChatRequest;
import com.sigo.chat.api.dto.ChatResponse;
import com.sigo.chat.application.service.ChatService;
import com.sigo.security.application.port.in.UsuarioActualUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
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

        return ResponseEntity.ok(
                new ChatResponse(
                        chatService.procesar(
                                request.message(),
                                request.historySegura()
                        )
                )
        );
    }
}
