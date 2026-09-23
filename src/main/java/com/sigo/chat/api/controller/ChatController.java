package com.sigo.chat.api.controller;

import com.sigo.chat.api.dto.ChatRequest;
import com.sigo.chat.api.dto.ChatResponse;
import com.sigo.chat.application.service.ChatService;
import com.sigo.personal.infrastructure.persistence.entity.RolSistema;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.security.application.service.CurrentUserService;
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
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        Trabajador usuario = currentUserService.requireCurrent();
        RolSistema rol = usuario.getRolSistema();

        if (rol != RolSistema.SUPERVISOR && rol != RolSistema.CONTROLADOR) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El Asistente SIGO está disponible solo para supervisores y controladores"
            );
        }

        return ResponseEntity.ok(new ChatResponse(
                chatService.procesar(request.message(), request.historySegura())
        ));
    }
}
