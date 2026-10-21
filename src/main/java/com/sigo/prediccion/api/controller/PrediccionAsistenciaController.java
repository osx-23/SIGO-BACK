package com.sigo.prediccion.api.controller;

import com.sigo.prediccion.api.dto.PrediccionAsistenciaRequest;
import com.sigo.prediccion.api.dto.PrediccionAsistenciaResponse;
import com.sigo.prediccion.application.PrediccionAsistenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/asistencia")
@RequiredArgsConstructor
public class PrediccionAsistenciaController {
    private final PrediccionAsistenciaService service;

    // Protegido por el matcher /api/dashboard/** del SecurityConfig:
    // solo SUPERVISOR y CONTROLADOR con JWT valido.
    @PostMapping("/predecir")
    public ResponseEntity<PrediccionAsistenciaResponse> predecir(
            @Valid @RequestBody PrediccionAsistenciaRequest request
    ) {
        return ResponseEntity.ok(service.predecir(request));
    }
}
