package com.sigo.asistencia.inventario.security;

import com.sigo.asistencia.inventario.entity.InventarioPuestoRol;
import com.sigo.asistencia.inventario.entity.InventarioRol;
import com.sigo.asistencia.inventario.repository.InventarioPuestoRolRepository;
import com.sigo.asistencia.inventario.repository.InventarioRolRepository;
import com.sigo.asistencia.personal.entity.RolSistema;
import com.sigo.asistencia.personal.entity.Trabajador;
import com.sigo.asistencia.security.service.CurrentUserService;
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
