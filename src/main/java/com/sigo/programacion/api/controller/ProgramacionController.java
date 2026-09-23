package com.sigo.programacion.api.controller;

import com.sigo.programacion.application.port.in.AsignarLiderUseCase;
import com.sigo.programacion.application.port.in.AsignarSecuenciaUseCase;
import com.sigo.programacion.application.port.in.GuardarOrdenSecuenciaUseCase;
import com.sigo.programacion.application.port.in.GuardarTurnosUseCase;
import com.sigo.programacion.application.port.in.ListarLideresUseCase;
import com.sigo.programacion.application.port.in.ListarTurnosUseCase;
import com.sigo.programacion.application.port.in.MiHorarioUseCase;
import com.sigo.programacion.application.port.in.ListarSecuenciasUseCase;
import com.sigo.programacion.api.dto.AsignarSecuenciaRequest;
import com.sigo.programacion.api.dto.GrupoLiderRequest;
import com.sigo.programacion.api.dto.GrupoLiderResponse;
import com.sigo.programacion.api.dto.GuardarOrdenSecuenciaRequest;
import com.sigo.programacion.api.dto.GuardarProgramacionRequest;
import com.sigo.programacion.api.dto.ProgramacionDiaResponse;
import com.sigo.programacion.api.dto.SecuenciaAgenteResponse;
import com.sigo.programacion.api.dto.MiHorarioResponse;
import com.sigo.programacion.api.dto.ProgramacionContextoResponse;
import com.sigo.programacion.api.dto.AgenteProgramacionExcepcionResponse;
import com.sigo.personal.api.dto.TrabajadorPublicResponse;
import com.sigo.personal.application.port.in.TrabajadorUseCase;
import com.sigo.programacion.application.port.in.AgenteProgramacionExcepcionUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;


@RestController
@RequestMapping("/api/programacion")
@RequiredArgsConstructor
public class ProgramacionController {

    private final GuardarOrdenSecuenciaUseCase guardarOrdenSecuenciaUseCase;

    private final ListarSecuenciasUseCase listarSecuenciasUseCase;

    private final AsignarSecuenciaUseCase asignarSecuenciaUseCase;

    private final ListarLideresUseCase listarLideresUseCase;

    private final AsignarLiderUseCase asignarLiderUseCase;

    private final ListarTurnosUseCase listarTurnosUseCase;

    private final GuardarTurnosUseCase guardarTurnosUseCase;

    private final MiHorarioUseCase miHorarioUseCase;

    private final TrabajadorUseCase trabajadorUseCase;

    private final AgenteProgramacionExcepcionUseCase excepcionUseCase;


    /*
     * ============================================================
     * CONTEXTO MENSUAL
     * ============================================================
     */

    @GetMapping("/contexto")
    public ProgramacionContextoResponse contexto(
            @RequestParam Long plazaId,
            @RequestParam int anio,
            @RequestParam int mes
    ) {
        /*
         * Cada use case conserva sus validaciones de seguridad.
         * DelegatingSecurityContextExecutor propaga el JWT/roles
         * al ejecutar las cuatro lecturas en virtual threads.
         */
        try (var virtualExecutor =
                     Executors.newVirtualThreadPerTaskExecutor()) {

            var executor =
                    new DelegatingSecurityContextExecutor(
                            virtualExecutor
                    );

            var agentesFuture =
                    CompletableFuture.supplyAsync(
                            () -> trabajadorUseCase
                                    .listarAgentesPorPlaza(plazaId),
                            executor
                    );

            var turnosFuture =
                    CompletableFuture.supplyAsync(
                            () -> listarTurnosUseCase
                                    .listar(plazaId, anio, mes),
                            executor
                    );

            var secuenciasFuture =
                    CompletableFuture.supplyAsync(
                            () -> listarSecuenciasUseCase
                                    .listar(plazaId),
                            executor
                    );

            var excepcionesFuture =
                    CompletableFuture.supplyAsync(
                            () -> excepcionUseCase
                                    .listarPorPlaza(plazaId),
                            executor
                    );

            CompletableFuture
                    .allOf(
                            agentesFuture,
                            turnosFuture,
                            secuenciasFuture,
                            excepcionesFuture
                    )
                    .join();

            return new ProgramacionContextoResponse(
                    agentesFuture.join()
                            .stream()
                            .map(this::toTrabajadorResponse)
                            .toList(),
                    turnosFuture.join()
                            .stream()
                            .map(this::toProgramacionResponse)
                            .toList(),
                    secuenciasFuture.join()
                            .stream()
                            .map(this::toSecuenciaResponse)
                            .toList(),
                    excepcionesFuture.join()
                            .stream()
                            .map(this::toExcepcionResponse)
                            .toList()
            );
        }
    }


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
        return toMiHorarioResponse(
                miHorarioUseCase.obtener(
                        desde,
                        hasta
                )
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


    private MiHorarioResponse toMiHorarioResponse(
            MiHorarioUseCase.Horario horario
    ) {
        return new MiHorarioResponse(
                horario.trabajadorId(),
                horario.codigo(),
                horario.nombre(),
                horario.plazaId(),
                horario.plazaCodigo(),
                horario.lider(),
                horario.dias()
                        .stream()
                        .map(dia ->
                                new MiHorarioResponse.HorarioDiaResponse(
                                        dia.fecha(),
                                        dia.estado(),
                                        dia.ubicacionCodigo(),
                                        dia.ubicacionNombre()
                                )
                        )
                        .toList()
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


    private TrabajadorPublicResponse toTrabajadorResponse(
            TrabajadorUseCase.TrabajadorData trabajador
    ) {
        return new TrabajadorPublicResponse(
                trabajador.id(),
                trabajador.codigo(),
                trabajador.nombreCompleto(),
                trabajador.puesto() == null
                        ? null
                        : new TrabajadorPublicResponse.PuestoResponse(
                                trabajador.puesto().id(),
                                trabajador.puesto().nombre()
                        ),
                trabajador.plaza() == null
                        ? null
                        : new TrabajadorPublicResponse.PlazaResponse(
                                trabajador.plaza().id(),
                                trabajador.plaza().codigo(),
                                trabajador.plaza().descripcion(),
                                trabajador.plaza().activo()
                        ),
                trabajador.rolSistema(),
                trabajador.requiereCambioPassword(),
                trabajador.activo()
        );
    }


    private AgenteProgramacionExcepcionResponse toExcepcionResponse(
            AgenteProgramacionExcepcionUseCase.Excepcion excepcion
    ) {
        return new AgenteProgramacionExcepcionResponse(
                excepcion.id(),
                excepcion.trabajadorId(),
                excepcion.codigoTrabajador(),
                excepcion.nombreTrabajador(),
                excepcion.plazaId(),
                excepcion.plazaCodigo(),
                excepcion.permiteA(),
                excepcion.permiteB(),
                excepcion.permiteC(),
                excepcion.motivo(),
                excepcion.color(),
                excepcion.activo(),
                excepcion.createdAt(),
                excepcion.updatedAt()
        );
    }

}
