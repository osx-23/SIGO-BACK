package com.sigo.relevo.api.controller;

import com.sigo.relevo.api.dto.ElementoRelevoResponse;
import com.sigo.relevo.api.dto.EvidenciaRelevoResponse;
import com.sigo.relevo.api.dto.RelevoChecklistResponse;
import com.sigo.relevo.api.dto.RelevoRequest;
import com.sigo.relevo.api.dto.RelevoResponse;
import com.sigo.relevo.api.dto.RelevoViaResponse;
import com.sigo.relevo.application.port.in.ConsultarRelevosUseCase;
import com.sigo.relevo.application.port.in.GestionarEvidenciaRelevoUseCase;
import com.sigo.relevo.application.port.in.GestionarRelevoUseCase;
import com.sigo.relevo.application.port.in.RelevoAccesoUseCase;
import com.sigo.relevo.application.port.in.RelevoHistorialUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/relevos")
@RequiredArgsConstructor
public class RelevoController {

    private final ConsultarRelevosUseCase consultaUseCase;
    private final GestionarRelevoUseCase gestionarUseCase;
    private final GestionarEvidenciaRelevoUseCase evidenciaUseCase;
    private final RelevoHistorialUseCase historialUseCase;
    private final RelevoAccesoUseCase accesoUseCase;

    @GetMapping("/elementos")
    public List<ElementoRelevoResponse> elementos() {
        return consultaUseCase
                .listarElementos()
                .stream()
                .map(elemento ->
                        new ElementoRelevoResponse(
                                elemento.id(),
                                elemento.codigo(),
                                elemento.nombre(),
                                elemento.categoria(),
                                elemento.requiereCantidad(),
                                elemento.orden()
                        )
                )
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RelevoResponse registrar(
            @Valid @RequestBody RelevoRequest request
    ) {
        return toResponse(
                gestionarUseCase.registrar(
                        accesoUseCase.prepararRegistro(
                                toCommand(request)
                        )
                )
        );
    }

    @PutMapping("/{id}")
    public RelevoResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody RelevoRequest request
    ) {
        accesoUseCase.exigirPuedeActualizar();

        return toResponse(
                gestionarUseCase.actualizar(
                        id,
                        toCommand(request)
                )
        );
    }

    @GetMapping("/{id}")
    public RelevoResponse obtener(
            @PathVariable Long id
    ) {
        return toResponse(
                historialUseCase.obtenerPara(
                        accesoUseCase.usuarioActual(),
                        id
                )
        );
    }

    @GetMapping
    public List<RelevoResponse> listar(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate inicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fin
    ) {
        return historialUseCase
                .listarPara(
                        accesoUseCase.usuarioActual(),
                        inicio,
                        fin
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping(
            value = "/checklist/{checklistId}/evidencias",
            consumes = "multipart/form-data"
    )
    @ResponseStatus(HttpStatus.CREATED)
    public EvidenciaRelevoResponse evidenciaChecklist(
            @PathVariable Long checklistId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        return toEvidenciaResponse(
                evidenciaUseCase.guardarChecklist(
                        checklistId,
                        archivo(file)
                )
        );
    }

    @DeleteMapping(
            "/checklist/{checklistId}/evidencias/{evidenciaId}"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarEvidenciaChecklist(
            @PathVariable Long checklistId,
            @PathVariable Long evidenciaId
    ) throws IOException {
        evidenciaUseCase.eliminarChecklist(
                checklistId,
                evidenciaId
        );
    }

    @PostMapping(
            value = "/vias/{relevoViaId}/evidencias",
            consumes = "multipart/form-data"
    )
    @ResponseStatus(HttpStatus.CREATED)
    public EvidenciaRelevoResponse evidenciaVia(
            @PathVariable Long relevoViaId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        return toEvidenciaResponse(
                evidenciaUseCase.guardarVia(
                        relevoViaId,
                        archivo(file)
                )
        );
    }

    @DeleteMapping(
            "/vias/{relevoViaId}/evidencias/{evidenciaId}"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarEvidenciaVia(
            @PathVariable Long relevoViaId,
            @PathVariable Long evidenciaId
    ) throws IOException {
        evidenciaUseCase.eliminarVia(
                relevoViaId,
                evidenciaId
        );
    }

    private GestionarRelevoUseCase.Command toCommand(
            RelevoRequest request
    ) {
        return new GestionarRelevoUseCase.Command(
                request.plazaId(),
                request.turnoId(),
                request.operadorId(),
                request.fecha(),
                request.hora(),
                request.observaciones(),
                request.resumen(),
                request.checklist()
                        .stream()
                        .map(item ->
                                new GestionarRelevoUseCase.ChecklistItem(
                                        item.elementoId(),
                                        item.estado(),
                                        item.detalle(),
                                        item.cantidad()
                                )
                        )
                        .toList(),
                request.vias() == null
                        ? List.of()
                        : request.vias()
                                .stream()
                                .map(item ->
                                        new GestionarRelevoUseCase.ViaItem(
                                                item.viaId(),
                                                item.estado(),
                                                item.detalle()
                                        )
                                )
                                .toList()
        );
    }

    private RelevoResponse toResponse(
            ConsultarRelevosUseCase.Relevo relevo
    ) {
        return new RelevoResponse(
                relevo.id(),
                relevo.plazaId(),
                relevo.plazaCodigo(),
                relevo.plazaDescripcion(),
                relevo.turnoId(),
                relevo.turnoCodigo(),
                relevo.turnoNombre(),
                relevo.operadorId(),
                relevo.operadorCodigo(),
                relevo.operadorNombre(),
                relevo.fecha(),
                relevo.hora(),
                relevo.observaciones(),
                relevo.resumen(),
                relevo.createdAt(),
                relevo.updatedAt(),
                relevo.checklist()
                        .stream()
                        .map(item ->
                                new RelevoChecklistResponse(
                                        item.id(),
                                        item.elementoId(),
                                        item.codigo(),
                                        item.nombre(),
                                        item.categoria(),
                                        item.estado(),
                                        item.detalle(),
                                        item.cantidad(),
                                        item.evidencias()
                                                .stream()
                                                .map(this::toEvidenciaResponse)
                                                .toList()
                                )
                        )
                        .toList(),
                relevo.vias()
                        .stream()
                        .map(item ->
                                new RelevoViaResponse(
                                        item.id(),
                                        item.viaId(),
                                        item.numero(),
                                        item.nombre(),
                                        item.estado(),
                                        item.detalle(),
                                        item.evidencias()
                                                .stream()
                                                .map(this::toEvidenciaResponse)
                                                .toList()
                                )
                        )
                        .toList()
        );
    }

    private EvidenciaRelevoResponse toEvidenciaResponse(
            ConsultarRelevosUseCase.Evidencia evidencia
    ) {
        return new EvidenciaRelevoResponse(
                evidencia.id(),
                evidencia.urlArchivo(),
                evidencia.publicId(),
                evidencia.tipo(),
                evidencia.createdAt()
        );
    }

    private EvidenciaRelevoResponse toEvidenciaResponse(
            GestionarEvidenciaRelevoUseCase.Evidencia evidencia
    ) {
        return new EvidenciaRelevoResponse(
                evidencia.id(),
                evidencia.urlArchivo(),
                evidencia.publicId(),
                evidencia.tipo(),
                evidencia.createdAt()
        );
    }

    private GestionarEvidenciaRelevoUseCase.ArchivoEntrada archivo(
            MultipartFile file
    ) throws IOException {
        return new GestionarEvidenciaRelevoUseCase.ArchivoEntrada(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes()
        );
    }
}
