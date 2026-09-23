package com.sigo.programacion.api.controller;

import com.sigo.programacion.domain.SecuenciaOrdenInvalidoException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(
        basePackageClasses = ProgramacionController.class
)
public class ProgramacionExceptionHandler {

    @ExceptionHandler(SecuenciaOrdenInvalidoException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleSecuenciaOrdenInvalido(
            SecuenciaOrdenInvalidoException exception
    ) {
        return Map.of(
                "message",
                exception.getMessage()
        );
    }
}
