package com.sigo.security.api.controller;

import com.sigo.security.domain.AutenticacionException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackageClasses = AuthController.class)
public class AuthExceptionHandler {

    @ExceptionHandler(AutenticacionException.class)
    public org.springframework.http.ResponseEntity<Map<String, String>>
    handle(AutenticacionException exception) {
        HttpStatus status = switch (exception.tipo()) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
        };

        return org.springframework.http.ResponseEntity
                .status(status)
                .body(
                        Map.of(
                                "message",
                                exception.getMessage()
                        )
                );
    }
}
