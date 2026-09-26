package com.sigo.incidencia.api.controller;

import com.sigo.incidencia.api.dto.IncidenciaRequest;
import com.sigo.incidencia.api.dto.IncidenciaResponse;
import com.sigo.incidencia.application.port.in.IncidenciaUseCase;
import com.sigo.incidencia.domain.EstadoIncidencia;
import com.sigo.shared.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidencias")
@RequiredArgsConstructor
public class IncidenciaController {

    private final IncidenciaUseCase useCase;

    @GetMapping("/tipos")
    public List<IncidenciaUseCase.Tipo> tipos() {
        return useCase.listarTipos();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidenciaResponse registrar(
            @Valid @RequestBody IncidenciaRequest request
    ) {
        IncidenciaUseCase.Command command =
                new IncidenciaUseCase.Command(
                        request.plazaId(),
                        request.turnoId(),
                        request.tipoId(),
                        request.viaId(),
                        request.fecha(),
                        request.hora(),
                        request.descripcion()
                );

        return map(
                useCase.registrar(command)
        );
    }

    @GetMapping
    public List<IncidenciaResponse> listar(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate inicio,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fin,

            @RequestParam(required = false)
            Long plazaId,

            @RequestParam(required = false)
            EstadoIncidencia estado
    ) {
        return useCase
                .listar(
                        inicio,
                        fin,
                        plazaId,
                        estado
                )
                .stream()
                .map(this::map)
                .toList();
    }

    @GetMapping("/{id}")
    public IncidenciaResponse obtener(
            @PathVariable Long id
    ) {
        return map(
                useCase.obtener(id)
        );
    }

    @PostMapping("/{id}/evidencias")
    @ResponseStatus(HttpStatus.CREATED)
    public IncidenciaResponse.Evidencia subirEvidencia(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            return map(
                    useCase.subirEvidencia(
                            id,
                            file.getBytes(),
                            file.getContentType()
                    )
            );
        } catch (IOException e) {
            throw new BusinessException(
                    "No se pudo leer la evidencia"
            );
        }
    }

    @PatchMapping("/{id}/atender")
    @PreAuthorize(
            "hasAnyRole('SUPERVISOR','CONTROLADOR')"
    )
    public IncidenciaResponse atender(
            @PathVariable Long id
    ) {
        return map(
                useCase.atender(id)
        );
    }

    @GetMapping("/pendientes/count")
    @PreAuthorize(
            "hasAnyRole('SUPERVISOR','CONTROLADOR')"
    )
    public Map<String, Long> pendientes() {
        return Map.of(
                "cantidad",
                useCase.contarPendientes()
        );
    }

    private IncidenciaResponse map(
            IncidenciaUseCase.Incidencia i
    ) {
        return new IncidenciaResponse(
                i.id(),
                i.plazaId(),
                i.plazaCodigo(),
                i.turnoId(),
                i.turnoCodigo(),
                i.registradoPorId(),
                i.registradoPorCodigo(),
                i.registradoPorNombre(),
                i.tipoId(),
                i.tipoNombre(),
                i.viaId(),
                i.viaNumero(),
                i.viaNombre(),
                i.fecha(),
                i.hora(),
                i.descripcion(),
                i.estado(),
                i.fechaCreacion(),
                i.fechaAtendido(),
                i.evidencias()
                        .stream()
                        .map(this::map)
                        .toList()
        );
    }

    private IncidenciaResponse.Evidencia map(
            IncidenciaUseCase.Evidencia e
    ) {
        return new IncidenciaResponse.Evidencia(
                e.id(),
                e.urlArchivo(),
                e.publicId(),
                e.tipo(),
                e.fechaCreacion()
        );
    }
}
