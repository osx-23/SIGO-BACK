package com.sigo.inventario.application.security;

import com.sigo.inventario.infrastructure.persistence.entity.InventarioPuestoRol;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioRol;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioPuestoRolRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioRolRepository;
import com.sigo.personal.infrastructure.persistence.entity.RolSistema;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.security.application.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class InventarioUsuarioContextService {

    private final CurrentUserService currentUserService;
    private final InventarioPuestoRolRepository puestoRolRepository;
    private final InventarioRolRepository inventarioRolRepository;

    public InventarioUsuarioActual obtenerActual() {
        Trabajador t = currentUserService.requireCurrent();

        /*
         * El rol global autenticado prevalece para CONTROLADOR y SUPERVISOR.
         * Así la autorización del módulo queda alineada con Spring Security/JWT.
         * OPERADOR conserva el mapeo puesto -> rol para soportar los distintos
         * puestos operativos (Agente, Part Time, Suplencia, etc.).
         */
        if (t.getRolSistema() == RolSistema.SUPERVISOR) {
            return new InventarioUsuarioActual(t, rolActivo("SUPERVISOR"));
        }

        if (t.getRolSistema() == RolSistema.CONTROLADOR) {
            return new InventarioUsuarioActual(t, rolActivo("CONTROLADOR"));
        }

        if (t.getPuesto() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Trabajador sin puesto asignado");
        }

        InventarioPuestoRol pr = puestoRolRepository.findByPuestoId(t.getPuesto().getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "El puesto no tiene rol de Inventario"
                ));

        if (!Boolean.TRUE.equals(pr.getRol().getActivo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rol de inventario inactivo");
        }

        return new InventarioUsuarioActual(t, pr.getRol());
    }

    private InventarioRol rolActivo(String codigo) {
        return inventarioRolRepository.findByCodigoAndActivoTrue(codigo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No existe un rol activo de Inventario para " + codigo
                ));
    }
}
