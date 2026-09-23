package com.sigo.asistencia.programacion.controller;

import com.sigo.asistencia.programacion.dto.generador.ProgramacionGeneradorDto.GenerarProgramacionRequest;
import com.sigo.asistencia.programacion.dto.generador.ProgramacionGeneradorDto.ProgramacionPropuestaResponse;
import com.sigo.asistencia.programacion.service.ProgramacionGeneradorService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/programacion")
@RequiredArgsConstructor
public class ProgramacionGeneradorController {

    private final ProgramacionGeneradorService generadorService;

    @PostMapping("/generar-propuesta")
    public ProgramacionPropuestaResponse generarPropuesta(
            @Valid
            @RequestBody GenerarProgramacionRequest request
    ) {

        return generadorService.generar(
                request
        );
    }
}