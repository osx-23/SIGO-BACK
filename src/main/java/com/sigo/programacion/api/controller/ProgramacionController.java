package com.sigo.programacion.api.controller;

import com.sigo.programacion.application.port.in.AsignarLiderUseCase;
import com.sigo.programacion.application.port.in.AsignarSecuenciaUseCase;
import com.sigo.programacion.application.port.in.GuardarOrdenSecuenciaUseCase;
import com.sigo.programacion.application.port.in.GuardarTurnosUseCase;
import com.sigo.programacion.application.port.in.ListarLideresUseCase;
import com.sigo.programacion.application.port.in.ListarTurnosUseCase;
import com.sigo.programacion.application.port.in.ListarSecuenciasUseCase;
import com.sigo.programacion.application.service.ProgramacionService;
import com.sigo.programacion.api.dto.AsignarSecuenciaRequest;
import com.sigo.programacion.api.dto.GrupoLiderRequest;
import com.sigo.programacion.api.dto.GrupoLiderResponse;
import com.sigo.programacion.api.dto.GuardarOrdenSecuenciaRequest;
import com.sigo.programacion.api.dto.GuardarProgramacionRequest;
import com.sigo.programacion.api.dto.ProgramacionDiaResponse;
import com.sigo.programacion.api.dto.SecuenciaAgenteResponse;
import com.sigo.programacion.application.service.ProgramacionService.MiHorarioResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/api/programacion")
@RequiredArgsConstructor
public class ProgramacionController {

    private final ProgramacionService programacionService;

    private final GuardarOrdenSecuenciaUseCase guardarOrdenSecuenciaUseCase;

    private final ListarSecuenciasUseCase listarSecuenciasUseCase;

    private final AsignarSecuenciaUseCase asignarSecuenciaUseCase;

    private final ListarLideresUseCase listarLideresUseCase;

    private final AsignarLiderUseCase asignarLiderUseCase;

    private final ListarTurnosUseCase listarTurnosUseCase;

    private final GuardarTurnosUseCase guardarTurnosUseCase;


    /*
     * ============================================================
     * PROGRAMACIÓN DE TURNOS
     * ============================================================
     */

    @GetMapping("/turnos")
    public List<ProgramacionDiaResponse> listarTurnos(
            @RequestParam Long plazaId,
            @RequestParam int anio,
            @RequestParam int mes
    ) {
        return listarTurnosUseCase
                .listar(plazaId, anio, mes)
                .stream()
                .map(this::toProgramacionResponse)
                .toList();
    }


    @PutMapping("/turnos")
    public List<ProgramacionDiaResponse> guardarTurnos(
            @Valid
            @RequestBody GuardarProgramacionRequest request
    ) {
        var command = new GuardarTurnosUseCase.Command(
                request.plazaId(),
                request.programaciones()
                        .stream()
                        .map(item ->
                                new GuardarTurnosUseCase.Item(
                                        item.trabajadorId(),
                                        item.fecha(),
                                        item.estado()
                                )
                        )
                        .toList()
        );

        return guardarTurnosUseCase
                .guardar(command)
                .stream()
                .map(this::toProgramacionResponse)
                .toList();
    }


    /*
     * ============================================================
     * MI HORARIO
     * ============================================================
     */

    @GetMapping("/mi-horario")
    public MiHorarioResponse miHorario(
            @RequestParam LocalDate desde,
            @RequestParam LocalDate hasta
    ) {
        return programacionService.miHorario(
                desde,
                hasta
        );
    }


    /*
     * ============================================================
     * LÍDERES
     * ============================================================
     */

    @GetMapping("/grupos")
    public List<GrupoLiderResponse> listarLideres(
            @RequestParam Long plazaId
    ) {
        return listarLideresUseCase
                .listar(plazaId)
                .stream()
                .map(this::toLiderResponse)
                .toList();
    }


    @PutMapping("/grupos/lider")
    public GrupoLiderResponse asignarLider(
            @Valid
            @RequestBody GrupoLiderRequest request
    ) {
        var lider = asignarLiderUseCase.asignar(
                new AsignarLiderUseCase.Command(
                        request.agenteId(),
                        request.controladorId(),
                        request.plazaId(),
                        request.fechaInicio()
                )
        );

        return toLiderResponse(lider);
    }


    /*
     * ============================================================
     * SECUENCIAS
     * ============================================================
     */

    @GetMapping("/secuencias")
    public List<SecuenciaAgenteResponse> listarSecuencias(
            @RequestParam Long plazaId
    ) {
        return listarSecuenciasUseCase
                .listar(plazaId)
                .stream()
                .map(this::toSecuenciaResponse)
                .toList();
    }


    @PutMapping("/secuencias/asignar")
    public SecuenciaAgenteResponse asignarSecuencia(
            @Valid
            @RequestBody AsignarSecuenciaRequest request
    ) {
        var secuencia = asignarSecuenciaUseCase.asignar(
                new AsignarSecuenciaUseCase.Command(
                        request.agenteId(),
                        request.plazaId(),
                        request.grupo()
                )
        );

        return toSecuenciaResponse(secuencia);
    }


    @PutMapping("/secuencias/orden")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void guardarOrdenSecuencia(
            @Valid
            @RequestBody GuardarOrdenSecuenciaRequest request
    ) {
        guardarOrdenSecuenciaUseCase.guardar(
                new GuardarOrdenSecuenciaUseCase.Command(
                        request.plazaId(),
                        request.grupo(),
                        request.agentes()
                                .stream()
                                .map(item ->
                                        new GuardarOrdenSecuenciaUseCase.Item(
                                                item.agenteId(),
                                                item.orden()
                                        )
                                )
                                .toList()
                )
        );
    }


    private ProgramacionDiaResponse toProgramacionResponse(
            ListarTurnosUseCase.Turno turno
    ) {
        return new ProgramacionDiaResponse(
                turno.programacionId(),
                turno.trabajadorId(),
                turno.codigoTrabajador(),
                turno.nombreTrabajador(),
                turno.plazaId(),
                turno.plazaCodigo(),
                turno.fecha(),
                turno.estado()
        );
    }


    private GrupoLiderResponse toLiderResponse(
            ListarLideresUseCase.Lider lider
    ) {
        return new GrupoLiderResponse(
                lider.id(),
                lider.agenteId(),
                lider.agenteCodigo(),
                lider.agenteNombre(),
                lider.controladorId(),
                lider.controladorCodigo(),
                lider.controladorNombre(),
                lider.plazaId(),
                lider.plazaCodigo(),
                lider.fechaInicio(),
                lider.fechaFin(),
                lider.activo()
        );
    }


    private SecuenciaAgenteResponse toSecuenciaResponse(
            ListarSecuenciasUseCase.Secuencia secuencia
    ) {
        return new SecuenciaAgenteResponse(
                secuencia.id(),
                secuencia.agenteId(),
                secuencia.codigo(),
                secuencia.nombre(),
                secuencia.plazaId(),
                secuencia.plazaCodigo(),
                secuencia.grupo(),
                secuencia.orden()
        );
    }

}
