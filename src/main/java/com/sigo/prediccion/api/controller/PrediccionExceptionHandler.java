package com.sigo.prediccion.api.controller;

import com.sigo.prediccion.domain.PrediccionInvalidaException;
import com.sigo.prediccion.domain.PrediccionNoDisponibleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = {PrediccionAsistenciaController.class, ProyeccionFinMesController.class})
public class PrediccionExceptionHandler {
    @ExceptionHandler(PrediccionInvalidaException.class)
    public ResponseEntity<Map<String, String>> invalida(PrediccionInvalidaException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(PrediccionNoDisponibleException.class)
    public ResponseEntity<Map<String, String>> noDisponible(PrediccionNoDisponibleException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", e.getMessage()));
    }
}
