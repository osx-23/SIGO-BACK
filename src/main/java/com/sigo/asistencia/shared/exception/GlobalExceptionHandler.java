package com.sigo.asistencia.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

 @ExceptionHandler(ResourceNotFoundException.class)
 ResponseEntity<Map<String, Object>> nf(ResourceNotFoundException e) {
  return build(HttpStatus.NOT_FOUND, e.getMessage());
 }

 @ExceptionHandler({
         BusinessException.class,
         IllegalArgumentException.class
 })
 ResponseEntity<Map<String, Object>> bad(RuntimeException e) {
  return build(HttpStatus.BAD_REQUEST, e.getMessage());
 }

 @ExceptionHandler(MethodArgumentNotValidException.class)
 ResponseEntity<Map<String, Object>> val(MethodArgumentNotValidException e) {

  Map<String, String> x = new LinkedHashMap<>();

  e.getBindingResult()
          .getFieldErrors()
          .forEach(f -> x.put(f.getField(), f.getDefaultMessage()));

  Map<String, Object> b = new LinkedHashMap<>();
  b.put("timestamp", OffsetDateTime.now());
  b.put("status", 400);
  b.put("message", "Hay datos inválidos");
  b.put("fields", x);

  return ResponseEntity.badRequest().body(b);
 }

 @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
 ResponseEntity<Map<String, Object>> status(
         org.springframework.web.server.ResponseStatusException e
 ) {
  HttpStatusCode status = e.getStatusCode();

  Map<String, Object> b = new LinkedHashMap<>();
  b.put("timestamp", OffsetDateTime.now());
  b.put("status", status.value());
  b.put("error", status.toString());
  b.put("message", e.getReason());

  if (status.is5xxServerError()) {
   log.error("ResponseStatusException no controlada. status={} reason={}", status.value(), e.getReason(), e);
  }

  return ResponseEntity.status(status).body(b);
 }

 @ExceptionHandler(Exception.class)
 ResponseEntity<Map<String, Object>> gen(Exception e) {
  log.error("Error interno no controlado", e);
  return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
 }

 private ResponseEntity<Map<String, Object>> build(HttpStatus s, String m) {
  Map<String, Object> b = new LinkedHashMap<>();
  b.put("timestamp", OffsetDateTime.now());
  b.put("status", s.value());
  b.put("error", s.getReasonPhrase());
  b.put("message", m);
  return ResponseEntity.status(s).body(b);
 }
}
