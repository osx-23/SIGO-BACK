package com.sigo.asistencia.personal.controller;

import com.sigo.asistencia.personal.dto.TrabajadorAdminUpdateRequest;
import com.sigo.asistencia.personal.dto.TrabajadorResponse;
import com.sigo.asistencia.personal.entity.Trabajador;
import com.sigo.asistencia.personal.service.TrabajadorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trabajadores")
@RequiredArgsConstructor
public class TrabajadorController {

    private final TrabajadorService trabajadorService;

    /*
     * ============================================================
     * AGENTES POR PLAZA
     * ============================================================
     *
     * Devuelve únicamente agentes activos pertenecientes
     * a la plaza indicada.
     *
     * Ejemplo:
     * GET /api/trabajadores/agentes?plazaId=4
     */
    @GetMapping("/agentes")
    public ResponseEntity<List<Trabajador>> listarAgentesPorPlaza(
            @RequestParam Long plazaId
    ) {

        List<Trabajador> agentes =
                trabajadorService.listarAgentesPorPlaza(plazaId);

        return ResponseEntity.ok(agentes);
    }

    /*
     * ============================================================
     * CONTROLADORES POR PLAZA
     * ============================================================
     *
     * Devuelve únicamente controladores activos pertenecientes
     * a la plaza indicada.
     *
     * Ejemplo:
     * GET /api/trabajadores/controladores?plazaId=4
     */
    @GetMapping("/controladores")
    public ResponseEntity<List<Trabajador>> listarControladoresPorPlaza(
            @RequestParam Long plazaId
    ) {

        List<Trabajador> controladores =
                trabajadorService.listarControladoresPorPlaza(plazaId);

        return ResponseEntity.ok(controladores);
    }

    /*
     * ============================================================
     * ADMINISTRACIÓN DE USUARIOS
     * ============================================================
     *
     * Solo SUPERVISOR.
     *
     * Sin plazaId:
     *
     * GET /api/trabajadores/admin
     *
     * Devuelve todos los trabajadores.
     *
     *
     * Con plazaId:
     *
     * GET /api/trabajadores/admin?plazaId=4
     *
     * Devuelve únicamente trabajadores pertenecientes
     * a esa plaza.
     *
     * Incluye activos e inactivos para permitir que el supervisor
     * pueda reactivar trabajadores.
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<List<TrabajadorResponse>> listarAdministracion(
            @RequestParam(required = false) Long plazaId
    ) {

        List<TrabajadorResponse> trabajadores =
                trabajadorService.listarAdministracion(plazaId);

        return ResponseEntity.ok(trabajadores);
    }

    /*
     * ============================================================
     * ACTUALIZAR USUARIO
     * ============================================================
     *
     * Permite al supervisor:
     *
     * - Cambiar la plaza del trabajador.
     * - Activar trabajador.
     * - Inactivar trabajador.
     *
     * Ejemplo:
     *
     * PUT /api/trabajadores/admin/28
     *
     * Body:
     *
     * {
     *   "plazaId": 4,
     *   "activo": true
     * }
     *
     * La lógica de:
     *
     * - cerrar relaciones de líder
     * - mover la secuencia
     * - dejar al agente "Sin secuencia"
     *
     * se encuentra en TrabajadorService.
     */
    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<TrabajadorResponse> actualizarAdministracion(
            @PathVariable Long id,
            @Valid @RequestBody TrabajadorAdminUpdateRequest request
    ) {

        TrabajadorResponse trabajador =
                trabajadorService.actualizarAdministracion(id, request);

        return ResponseEntity.ok(trabajador);
    }
}