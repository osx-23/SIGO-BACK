package com.sigo.personal.api.controller;

import com.sigo.personal.api.dto.TrabajadorAdminCreateRequest;
import com.sigo.personal.api.dto.TrabajadorAdminUpdateRequest;
import com.sigo.personal.api.dto.TrabajadorPublicResponse;
import com.sigo.personal.api.dto.TrabajadorResponse;
import com.sigo.personal.application.port.in.TrabajadorUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trabajadores")
@RequiredArgsConstructor
public class TrabajadorController {

    private final TrabajadorUseCase useCase;

    @GetMapping("/agentes")
    public ResponseEntity<List<TrabajadorPublicResponse>> listarAgentesPorPlaza(
            @RequestParam Long plazaId
    ) {
        return ResponseEntity.ok(
                useCase.listarAgentesPorPlaza(plazaId)
                        .stream()
                        .map(this::toPublicResponse)
                        .toList()
        );
    }

    @GetMapping("/controladores")
    public ResponseEntity<List<TrabajadorPublicResponse>> listarControladoresPorPlaza(
            @RequestParam Long plazaId
    ) {
        return ResponseEntity.ok(
                useCase.listarControladoresPorPlaza(plazaId)
                        .stream()
                        .map(this::toPublicResponse)
                        .toList()
        );
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<List<TrabajadorResponse>> listarAdministracion(
            @RequestParam(required = false) Long plazaId
    ) {
        return ResponseEntity.ok(
                useCase.listarAdministracion(plazaId)
                        .stream()
                        .map(this::toAdminResponse)
                        .toList()
        );
    }

    @GetMapping("/admin/puestos")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<List<TrabajadorUseCase.PuestoData>> listarPuestos() {
        return ResponseEntity.ok(
                useCase.listarPuestosAdministrables()
        );
    }

    @PostMapping("/admin")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPERVISOR')")
    public TrabajadorResponse crearUsuario(
            @Valid
            @RequestBody TrabajadorAdminCreateRequest request
    ) {
        return toAdminResponse(
                useCase.crearUsuario(
                        request.codigo(),
                        request.nombreCompleto(),
                        request.puestoId(),
                        request.plazaId(),
                        request.passwordInicial()
                )
        );
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<TrabajadorResponse> actualizarAdministracion(
            @PathVariable Long id,
            @Valid
            @RequestBody TrabajadorAdminUpdateRequest request
    ) {
        return ResponseEntity.ok(
                toAdminResponse(
                        useCase.actualizarAdministracion(
                                id,
                                request.plazaId(),
                                request.puestoId(),
                                request.activo()
                        )
                )
        );
    }

    private TrabajadorResponse toAdminResponse(
            TrabajadorUseCase.TrabajadorData trabajador
    ) {
        return new TrabajadorResponse(
                trabajador.id(),
                trabajador.codigo(),
                trabajador.nombreCompleto(),
                trabajador.puesto() == null
                        ? null
                        : new TrabajadorResponse.PuestoResumen(
                                trabajador.puesto().id(),
                                trabajador.puesto().nombre()
                        ),
                trabajador.plaza() == null
                        ? null
                        : new TrabajadorResponse.PlazaResumen(
                                trabajador.plaza().id(),
                                trabajador.plaza().codigo(),
                                trabajador.plaza().descripcion()
                        ),
                trabajador.rolSistema(),
                trabajador.requiereCambioPassword(),
                trabajador.activo()
        );
    }

    private TrabajadorPublicResponse toPublicResponse(
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
}
