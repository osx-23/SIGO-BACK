package com.sigo.programacion.api.controller;

import com.sigo.programacion.domain.ProgramacionValidationException;
import com.sigo.programacion.domain.SecuenciaOrdenInvalidoException;
import org.springframework.http.HttpStatus;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Valida errores de programación antes del manejador global genérico (Exception.class). */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(
        basePackageClasses = {ProgramacionController.class, TrasladoPlazaController.class}
)
public class ProgramacionExceptionHandler {

    @ExceptionHandler(ProgramacionValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleProgramacionValidation(
            ProgramacionValidationException exception
    ) {
        return Map.of(
                "message",
                exception.getMessage()
        );
    }

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
