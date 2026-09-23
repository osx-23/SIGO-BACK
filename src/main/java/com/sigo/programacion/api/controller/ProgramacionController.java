package com.sigo.programacion.api.controller;

import com.sigo.programacion.application.port.in.AsignarSecuenciaUseCase;
import com.sigo.programacion.application.port.in.GuardarOrdenSecuenciaUseCase;
import com.sigo.programacion.application.port.in.ListarSecuenciasUseCase;
import com.sigo.programacion.application.service.ProgramacionService;
import com.sigo.programacion.application.service.ProgramacionService.GrupoLiderRequest;
import com.sigo.programacion.application.service.ProgramacionService.GrupoLiderResponse;
import com.sigo.programacion.api.dto.AsignarSecuenciaRequest;
import com.sigo.programacion.api.dto.GuardarOrdenSecuenciaRequest;
import com.sigo.programacion.api.dto.SecuenciaAgenteResponse;
import com.sigo.programacion.application.service.ProgramacionService.GuardarProgramacionRequest;
import com.sigo.programacion.application.service.ProgramacionService.MiHorarioResponse;
import com.sigo.programacion.application.service.ProgramacionService.ProgramacionDiaResponse;

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
        return programacionService.listarTurnos(
                plazaId,
                anio,
                mes
        );
    }


    @PutMapping("/turnos")
    public List<ProgramacionDiaResponse> guardarTurnos(
            @Valid
            @RequestBody GuardarProgramacionRequest request
    ) {
        return programacionService.guardarTurnos(
                request
        );
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
        return programacionService.listarLideres(
                plazaId
        );
    }


    @PutMapping("/grupos/lider")
    public GrupoLiderResponse asignarLider(
            @Valid
            @RequestBody GrupoLiderRequest request
    ) {
        return programacionService.asignarLider(
                request
        );
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
