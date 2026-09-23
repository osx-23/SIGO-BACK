package com.sigo.inventario.infrastructure.security;

import com.sigo.inventario.application.port.out.InventarioUsuarioContextPort;
import com.sigo.inventario.application.security.InventarioUsuarioActual;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioRol;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioPuestoRolRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioRolRepository;
import com.sigo.security.application.port.in.UsuarioActualUseCase;
import com.sigo.shared.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventarioUsuarioContextAdapter
        implements InventarioUsuarioContextPort {

    private final UsuarioActualUseCase usuarioActualUseCase;
    private final InventarioPuestoRolRepository puestoRolRepository;
    private final InventarioRolRepository inventarioRolRepository;

    @Override
    public InventarioUsuarioActual obtenerActual() {
        UsuarioActualUseCase.UsuarioActual trabajador =
                usuarioActualUseCase.requireActual();

        InventarioRol rolInventario =
                resolverRolInventario(trabajador);

        return new InventarioUsuarioActual(
                trabajador.id(),
                trabajador.codigo(),
                trabajador.nombre(),
                trabajador.plazaId(),
                trabajador.plazaCodigo(),
                rolInventario.getId(),
                rolInventario.getCodigo(),
                trabajador.rol()
        );
    }

    private InventarioRol resolverRolInventario(
            UsuarioActualUseCase.UsuarioActual trabajador
    ) {
        if ("SUPERVISOR".equals(trabajador.rol())) {
            return rolActivo("SUPERVISOR");
        }

        if ("CONTROLADOR".equals(trabajador.rol())) {
            return rolActivo("CONTROLADOR");
        }

        if (trabajador.puestoId() == null) {
            throw new ForbiddenException(
                    "Trabajador sin puesto asignado"
            );
        }

        var puestoRol = puestoRolRepository
                .findByPuestoId(
                        trabajador.puestoId()
                )
                .orElseThrow(() ->
                        new ForbiddenException(
                                "El puesto no tiene rol de Inventario"
                        )
                );

        if (!Boolean.TRUE.equals(
                puestoRol.getRol().getActivo()
        )) {
            throw new ForbiddenException(
                    "Rol de inventario inactivo"
            );
        }

        return puestoRol.getRol();
    }

    private InventarioRol rolActivo(String codigo) {
        return inventarioRolRepository
                .findByCodigoAndActivoTrue(codigo)
                .orElseThrow(() ->
                        new ForbiddenException(
                                "No existe un rol activo de Inventario para "
                                        + codigo
                        )
                );
    }
}
