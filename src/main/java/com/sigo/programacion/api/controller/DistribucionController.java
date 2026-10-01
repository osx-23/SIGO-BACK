package com.sigo.programacion.api.controller;

import com.sigo.programacion.api.dto.CoberturaUbicacionResponse;
import com.sigo.programacion.api.dto.DistribucionDiaResponse;
import com.sigo.programacion.api.dto.GuardarDistribucionRequest;
import com.sigo.programacion.api.dto.GuardarUbicacionRequest;
import com.sigo.programacion.api.dto.ResumenTrabajadorResponse;
import com.sigo.programacion.api.dto.UbicacionResponse;
import com.sigo.programacion.application.port.in.DistribucionUseCase;
import com.sigo.programacion.application.port.in.UbicacionUseCase;
import com.sigo.programacion.application.port.out.DistribucionReportePdfPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/distribucion")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPERVISOR','CONTROLADOR')")
public class DistribucionController {

    private final UbicacionUseCase ubicacionUseCase;
    private final DistribucionUseCase distribucionUseCase;
    private final DistribucionReportePdfPort pdfService;

    @GetMapping("/ubicaciones")
    public List<UbicacionResponse> ubicaciones(
            @RequestParam Long plazaId
    ) {
        return ubicacionUseCase
                .listarActivas(plazaId)
                .stream()
                .map(this::toUbicacionResponse)
                .toList();
    }

    @GetMapping("/ubicaciones/configuracion")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public List<UbicacionResponse> ubicacionesConfiguracion(
            @RequestParam Long plazaId
    ) {
        return ubicacionUseCase
                .listarConfiguracion(plazaId)
                .stream()
                .map(this::toUbicacionResponse)
                .toList();
    }

    @PostMapping("/ubicaciones")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public UbicacionResponse crearUbicacion(
            @Valid
            @RequestBody GuardarUbicacionRequest request
    ) {
        return toUbicacionResponse(
                ubicacionUseCase.crear(
                        toUbicacionCommand(request)
                )
        );
    }

    @PutMapping("/ubicaciones/{ubicacionId}")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public UbicacionResponse actualizarUbicacion(
            @PathVariable Long ubicacionId,
            @Valid
            @RequestBody GuardarUbicacionRequest request
    ) {
        return toUbicacionResponse(
                ubicacionUseCase.actualizar(
                        ubicacionId,
                        toUbicacionCommand(request)
                )
        );
    }

    @PatchMapping("/ubicaciones/{ubicacionId}/estado")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public UbicacionResponse cambiarEstadoUbicacion(
            @PathVariable Long ubicacionId,
            @RequestParam boolean activo
    ) {
        return toUbicacionResponse(
                ubicacionUseCase.cambiarEstado(
                        ubicacionId,
                        activo
                )
        );
    }

    @GetMapping
    public List<DistribucionDiaResponse> listar(
            @RequestParam Long plazaId,
            @RequestParam int anio,
            @RequestParam int mes
    ) {
        return distribucionUseCase
                .listar(plazaId, anio, mes)
                .stream()
                .map(this::toDistribucionResponse)
                .toList();
    }

    @PutMapping
    public List<DistribucionDiaResponse> guardar(
            @Valid
            @RequestBody GuardarDistribucionRequest request
    ) {
        DistribucionUseCase.Command command =
                new DistribucionUseCase.Command(
                        request.plazaId(),
                        request.distribuciones()
                                .stream()
                                .map(item ->
                                        new DistribucionUseCase.Item(
                                                item.programacionTurnoId(),
                                                item.ubicacionId(),
                                                item.observacion()
                                        )
                                )
                                .toList()
                );

        return distribucionUseCase
                .guardar(
                        command,
                        request.forzar()
                )
                .stream()
                .map(this::toDistribucionResponse)
                .toList();
    }

    @GetMapping("/resumen-trabajador/{trabajadorId}")
    public ResumenTrabajadorResponse resumen(
            @PathVariable Long trabajadorId,
            @RequestParam int anio,
            @RequestParam int mes
    ) {
        return toResumenResponse(
                distribucionUseCase.resumen(
                        trabajadorId,
                        anio,
                        mes
                )
        );
    }

    @GetMapping(
            value = "/reporte-trabajador/{trabajadorId}/pdf",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> reporteTrabajadorPdf(
            @PathVariable Long trabajadorId,
            @RequestParam LocalDate desde,
            @RequestParam LocalDate hasta
    ) {
        DistribucionUseCase.ReporteTrabajador reporte =
                distribucionUseCase.reporteTrabajador(
                        trabajadorId,
                        desde,
                        hasta
                );

        byte[] pdf =
                pdfService.generar(
                        reporte,
                        desde,
                        hasta
                );

        String archivo =
                "distribucion-"
                        + reporte.resumen().codigo()
                        + "-"
                        + desde
                        + "-"
                        + hasta
                        + ".pdf";

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + archivo + "\""
                )
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .body(pdf);
    }


    @GetMapping(
            value = "/reporte-plaza/pdf",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> reportePlazaPdf(
            @RequestParam Long plazaId,
            @RequestParam LocalDate desde,
            @RequestParam LocalDate hasta
    ) {
        DistribucionUseCase.ReportePlaza reporte =
                distribucionUseCase.reportePlaza(
                        plazaId,
                        desde,
                        hasta
                );

        byte[] pdf =
                pdfService.generarMatriz(
                        reporte
                );

        String archivo =
                "distribucion-"
                        + reporte.plazaCodigo()
                        + "-"
                        + desde
                        + "-"
                        + hasta
                        + ".pdf";

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + archivo + "\""
                )
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .body(pdf);
    }


    @GetMapping("/cobertura")
    public List<CoberturaUbicacionResponse> cobertura(
            @RequestParam Long plazaId,
            @RequestParam int anio,
            @RequestParam int mes
    ) {
        return distribucionUseCase
                .cobertura(plazaId, anio, mes)
                .stream()
                .map(item ->
                        new CoberturaUbicacionResponse(
                                item.ubicacionId(),
                                item.codigo(),
                                item.nombre(),
                                item.porDia()
                        )
                )
                .toList();
    }

    private UbicacionUseCase.Command toUbicacionCommand(
            GuardarUbicacionRequest request
    ) {
        return new UbicacionUseCase.Command(
                request.plazaId(),
                request.codigo(),
                request.nombre(),
                request.tipo(),
                request.orden(),
                request.permiteTurnoA(),
                request.permiteTurnoB(),
                request.permiteTurnoC()
        );
    }

    private UbicacionResponse toUbicacionResponse(
            UbicacionUseCase.Ubicacion ubicacion
    ) {
        return new UbicacionResponse(
                ubicacion.id(),
                ubicacion.plazaId(),
                ubicacion.codigo(),
                ubicacion.nombre(),
                ubicacion.tipo(),
                ubicacion.viaId(),
                ubicacion.activo(),
                ubicacion.orden(),
                ubicacion.permiteTurnoA(),
                ubicacion.permiteTurnoB(),
                ubicacion.permiteTurnoC()
        );
    }

    private DistribucionDiaResponse toDistribucionResponse(
            DistribucionUseCase.Distribucion distribucion
    ) {
        return new DistribucionDiaResponse(
                distribucion.distribucionId(),
                distribucion.programacionTurnoId(),
                distribucion.trabajadorId(),
                distribucion.codigoTrabajador(),
                distribucion.nombreTrabajador(),
                distribucion.fecha(),
                distribucion.estado(),
                distribucion.ubicacionId(),
                distribucion.ubicacionCodigo(),
                distribucion.ubicacionNombre(),
                distribucion.ubicacionTipo(),
                distribucion.observacion()
        );
    }

    private ResumenTrabajadorResponse toResumenResponse(
            DistribucionUseCase.ResumenTrabajador resumen
    ) {
        return new ResumenTrabajadorResponse(
                resumen.trabajadorId(),
                resumen.codigo(),
                resumen.nombre(),
                resumen.ubicaciones()
                        .stream()
                        .map(ubicacion ->
                                new ResumenTrabajadorResponse.ResumenUbicacionResponse(
                                        ubicacion.codigo(),
                                        ubicacion.nombre(),
                                        ubicacion.veces()
                                )
                        )
                        .toList()
        );
    }
}
