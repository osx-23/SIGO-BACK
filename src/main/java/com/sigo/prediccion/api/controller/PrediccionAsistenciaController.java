package com.sigo.prediccion.api.controller;

import com.sigo.prediccion.api.dto.PrediccionAsistenciaRequest;
import com.sigo.prediccion.api.dto.PrediccionAsistenciaResponse;
import com.sigo.prediccion.application.PrediccionAsistenciaService;
import com.sigo.prediccion.domain.PrediccionAsistenciaParametros;
import com.sigo.prediccion.domain.PrediccionAsistenciaResultado;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/asistencia")
@RequiredArgsConstructor
public class PrediccionAsistenciaController {
    private final PrediccionAsistenciaService service;

    @PostMapping("/predecir")
    public ResponseEntity<PrediccionAsistenciaResponse> predecir(
            @Valid @RequestBody PrediccionAsistenciaRequest request
    ) {
        PrediccionAsistenciaResultado result = service.predecir(
                new PrediccionAsistenciaParametros(
                        request.plazaId(), request.turnoId(),
                        request.fecha(), request.programados()
                )
        );
        return ResponseEntity.ok(new PrediccionAsistenciaResponse(
                result.plazaId(), result.turnoId(), result.fecha(), result.programados(),
                result.ausenciasEstimadas(), result.coberturaEstimada(), result.modelo(),
                result.version(), result.origenDatos(), result.advertencia()
        ));
    }
}
