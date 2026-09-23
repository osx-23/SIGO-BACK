package com.sigo.inventario.infrastructure.security;

import com.sigo.inventario.application.port.out.InventarioUsuarioContextPort;
import com.sigo.inventario.application.security.InventarioUsuarioActual;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioRol;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioPuestoRolRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioRolRepository;
import com.sigo.personal.infrastructure.persistence.entity.RolSistema;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.security.application.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class InventarioUsuarioContextAdapter
        implements InventarioUsuarioContextPort {

    private final CurrentUserService currentUserService;
    private final InventarioPuestoRolRepository puestoRolRepository;
    private final InventarioRolRepository inventarioRolRepository;

    @Override
    public InventarioUsuarioActual obtenerActual() {
        Trabajador trabajador =
                currentUserService.requireCurrent();

        InventarioRol rolInventario =
                resolverRolInventario(trabajador);

        return new InventarioUsuarioActual(
                trabajador.getId(),
                trabajador.getCodigo(),
                trabajador.getNombreCompleto(),
                trabajador.getPlaza() == null
                        ? null
                        : trabajador.getPlaza().getId(),
                trabajador.getPlaza() == null
                        ? null
                        : trabajador.getPlaza().getCodigo(),
                rolInventario.getId(),
                rolInventario.getCodigo(),
                trabajador.getRolSistema().name()
        );
    }

    private InventarioRol resolverRolInventario(
            Trabajador trabajador
    ) {
        if (trabajador.getRolSistema()
                == RolSistema.SUPERVISOR) {
            return rolActivo("SUPERVISOR");
        }

        if (trabajador.getRolSistema()
                == RolSistema.CONTROLADOR) {
            return rolActivo("CONTROLADOR");
        }

        if (trabajador.getPuesto() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Trabajador sin puesto asignado"
            );
        }

        var puestoRol = puestoRolRepository
                .findByPuestoId(
                        trabajador.getPuesto().getId()
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "El puesto no tiene rol de Inventario"
                        )
                );

        if (!Boolean.TRUE.equals(
                puestoRol.getRol().getActivo()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Rol de inventario inactivo"
            );
        }

        return puestoRol.getRol();
    }

    private InventarioRol rolActivo(String codigo) {
        return inventarioRolRepository
                .findByCodigoAndActivoTrue(codigo)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "No existe un rol activo de Inventario para "
                                        + codigo
                        )
                );
    }
}
