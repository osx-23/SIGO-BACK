package com.sigo.programacion.api.controller;

import com.sigo.programacion.api.dto.generador.ProgramacionGeneradorDto;
import com.sigo.programacion.application.port.in.GenerarProgramacionUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/programacion")
@RequiredArgsConstructor
public class ProgramacionGeneradorController {

    private final GenerarProgramacionUseCase useCase;

    @PostMapping("/generar-propuesta")
    public ProgramacionGeneradorDto.ProgramacionPropuestaResponse generarPropuesta(
            @Valid
            @RequestBody ProgramacionGeneradorDto.GenerarProgramacionRequest request
    ) {
        return toResponse(
                useCase.generar(
                        toCommand(request)
                )
        );
    }

    private GenerarProgramacionUseCase.GenerarProgramacionRequest toCommand(
            ProgramacionGeneradorDto.GenerarProgramacionRequest request
    ) {
        return new GenerarProgramacionUseCase.GenerarProgramacionRequest(
                request.plazaId(),
                request.anio(),
                request.mes(),
                toCobertura(request.coberturaNormal()),
                request.coberturaDomingo() == null
                        ? null
                        : toCobertura(request.coberturaDomingo()),
                request.diasEspeciales() == null
                        ? null
                        : request.diasEspeciales()
                                .stream()
                                .map(dia ->
                                        new GenerarProgramacionUseCase.DiaEspecialRequest(
                                                dia.fecha(),
                                                dia.descripcion(),
                                                dia.a(),
                                                dia.b(),
                                                dia.c()
                                        )
                                )
                                .toList(),
                request.novedades() == null
                        ? null
                        : request.novedades()
                                .stream()
                                .map(novedad ->
                                        new GenerarProgramacionUseCase.NovedadProgramacionRequest(
                                                novedad.trabajadorId(),
                                                novedad.estado(),
                                                novedad.desde(),
                                                novedad.hasta()
                                        )
                                )
                                .toList()
        );
    }

    private GenerarProgramacionUseCase.CoberturaTurnosRequest toCobertura(
            ProgramacionGeneradorDto.CoberturaTurnosRequest cobertura
    ) {
        return new GenerarProgramacionUseCase.CoberturaTurnosRequest(
                cobertura.a(),
                cobertura.b(),
                cobertura.c()
        );
    }

    private ProgramacionGeneradorDto.ProgramacionPropuestaResponse toResponse(
            GenerarProgramacionUseCase.ProgramacionPropuestaResponse response
    ) {
        return new ProgramacionGeneradorDto.ProgramacionPropuestaResponse(
                response.plazaId(),
                response.plazaCodigo(),
                response.anio(),
                response.mes(),
                response.guardado(),
                response.agentes()
                        .stream()
                        .map(agente ->
                                new ProgramacionGeneradorDto.ProgramacionAgentePropuesta(
                                        agente.trabajadorId(),
                                        agente.codigo(),
                                        agente.nombre(),
                                        agente.grupo(),
                                        agente.orden(),
                                        agente.partTime(),
                                        agente.dias()
                                                .stream()
                                                .map(dia ->
                                                        new ProgramacionGeneradorDto.ProgramacionDiaPropuesta(
                                                                dia.fecha(),
                                                                dia.estado(),
                                                                dia.estadoCiclo(),
                                                                dia.origen(),
                                                                dia.excepcion(),
                                                                dia.observacion()
                                                        )
                                                )
                                                .toList()
                                )
                        )
                        .toList(),
                response.cobertura()
                        .stream()
                        .map(cobertura ->
                                new ProgramacionGeneradorDto.CoberturaDiaResponse(
                                        cobertura.fecha(),
                                        cobertura.requeridoA(),
                                        cobertura.requeridoB(),
                                        cobertura.requeridoC(),
                                        cobertura.asignadoA(),
                                        cobertura.asignadoB(),
                                        cobertura.asignadoC(),
                                        cobertura.deficitA(),
                                        cobertura.deficitB(),
                                        cobertura.deficitC(),
                                        cobertura.excesoA(),
                                        cobertura.excesoB(),
                                        cobertura.excesoC(),
                                        cobertura.tipoCobertura()
                                )
                        )
                        .toList(),
                response.conflictos()
                        .stream()
                        .map(conflicto ->
                                new ProgramacionGeneradorDto.ConflictoProgramacionResponse(
                                        conflicto.tipo(),
                                        conflicto.nivel(),
                                        conflicto.trabajadorId(),
                                        conflicto.codigo(),
                                        conflicto.trabajador(),
                                        conflicto.fecha(),
                                        conflicto.mensaje()
                                )
                        )
                        .toList()
        );
    }
}
