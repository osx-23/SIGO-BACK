package com.sigo.relevo.api.controller;

import com.sigo.personal.application.port.in.CatalogoPersonalUseCase;
import com.sigo.relevo.application.port.in.ConfigurarViasAviUseCase;
import com.sigo.security.application.port.in.UsuarioActualUseCase;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/avi/admin")
@RequiredArgsConstructor
public class ViaAviAdminController {

    private final ConfigurarViasAviUseCase configurarViasAviUseCase;
    private final CatalogoPersonalUseCase catalogoPersonalUseCase;
    private final UsuarioActualUseCase usuarioActualUseCase;

    @GetMapping("/plazas")
    public List<PlazaAviResponse> plazas() {
        requireSupervisor();

        return catalogoPersonalUseCase
                .plazas()
                .stream()
                .filter(item ->
                        Boolean.TRUE.equals(item.activo())
                )
                .map(item ->
                        new PlazaAviResponse(
                                item.id(),
                                item.codigo(),
                                item.descripcion()
                        )
                )
                .toList();
    }

    @GetMapping("/vias")
    public List<ViaAviConfigResponse> vias(
            @RequestParam Long plazaId
    ) {
        return configurarViasAviUseCase
                .listar(plazaId)
                .stream()
                .map(item ->
                        new ViaAviConfigResponse(
                                item.id(),
                                item.plazaId(),
                                item.numero(),
                                item.nombre(),
                                item.aviVisible()
                        )
                )
                .toList();
    }

    @PutMapping("/vias/{viaId}")
    public ViaAviConfigResponse actualizar(
            @PathVariable Long viaId,
            @RequestBody ViaAviConfigRequest request
    ) {
        if (
                request == null ||
                request.plazaId() == null ||
                request.visible() == null
        ) {
            throw new BusinessException(
                    "Plaza y visibilidad son obligatorias"
            );
        }

        configurarViasAviUseCase.actualizar(
                request.plazaId(),
                viaId,
                request.visible()
        );

        return configurarViasAviUseCase
                .listar(request.plazaId())
                .stream()
                .filter(item ->
                        item.id().equals(viaId)
                )
                .findFirst()
                .map(item ->
                        new ViaAviConfigResponse(
                                item.id(),
                                item.plazaId(),
                                item.numero(),
                                item.nombre(),
                                item.aviVisible()
                        )
                )
                .orElseThrow();
    }

    private void requireSupervisor() {
        UsuarioActualUseCase.UsuarioActual actual =
                usuarioActualUseCase.requireActual();

        if (
                actual.rol() == null ||
                !"SUPERVISOR".equalsIgnoreCase(
                        actual.rol()
                )
        ) {
            throw new ForbiddenException(
                    "Solo un supervisor puede administrar la configuración AVIX"
            );
        }
    }

    public record PlazaAviResponse(
            Long id,
            String codigo,
            String descripcion
    ) {
    }

    public record ViaAviConfigResponse(
            Long id,
            Long plazaId,
            Integer numero,
            String nombre,
            Boolean visible
    ) {
    }

    public record ViaAviConfigRequest(
            Long plazaId,
            Boolean visible
    ) {
    }
}
