package com.sigo.personal.service;

import com.sigo.personal.dto.TrabajadorAdminUpdateRequest;
import com.sigo.personal.dto.TrabajadorResponse;
import com.sigo.personal.entity.Plaza;
import com.sigo.personal.entity.Trabajador;
import com.sigo.personal.repository.PlazaRepository;
import com.sigo.personal.repository.TrabajadorRepository;
import com.sigo.programacion.repository.AgenteControladorLiderRepository;
import com.sigo.programacion.repository.ProgramacionSecuenciaAgenteRepository;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TrabajadorService {

    private final TrabajadorRepository trabajadorRepository;
    private final PlazaRepository plazaRepository;
    private final ProgramacionSecuenciaAgenteRepository secuenciaRepository;
    private final AgenteControladorLiderRepository liderRepository;

    /*
     * ============================================================
     * AGENTES
     * ============================================================
     */

    @Transactional(readOnly = true)
    public List<Trabajador> listarAgentesPorPlaza(Long plazaId) {
        return trabajadorRepository.findAgentesByPlaza(plazaId);
    }

    /*
     * ============================================================
     * CONTROLADORES
     * ============================================================
     */

    @Transactional(readOnly = true)
    public List<Trabajador> listarControladoresPorPlaza(Long plazaId) {
        return trabajadorRepository.findControladoresByPlaza(plazaId);
    }

    /*
     * ============================================================
     * ADMINISTRACIÓN DE USUARIOS
     * ============================================================
     */

    /**
     * Lista administrativa.
     *
     * Incluye trabajadores activos e inactivos para que un supervisor
     * pueda reactivar posteriormente a un trabajador.
     *
     * La consulta usa JOIN FETCH para cargar puesto y plaza en la misma
     * consulta y evitar consultas N+1.
     *
     * @param plazaId plaza por la que se desea filtrar.
     *                Si es null, devuelve todas las plazas.
     */
    @Transactional(readOnly = true)
    public List<TrabajadorResponse> listarAdministracion(Long plazaId) {

        List<Trabajador> trabajadores;

        if (plazaId == null) {
            trabajadores = trabajadorRepository.findAllAdminOptimizado();
        } else {
            trabajadores = trabajadorRepository.findAllAdminByPlazaOptimizado(plazaId);
        }

        return trabajadores.stream()
                .map(TrabajadorResponse::from)
                .toList();
    }

    /*
     * ============================================================
     * ACTUALIZAR PLAZA / ESTADO
     * ============================================================
     */

    /**
     * Permite al supervisor:
     *
     * - Cambiar la plaza de un trabajador.
     * - Activarlo.
     * - Inactivarlo.
     *
     * IMPORTANTE:
     *
     * Las relaciones de líder se cierran ANTES de cambiar la plaza
     * o inactivar al trabajador.
     *
     * Esto es necesario porque la base de datos posee el trigger
     * validar_agente_controlador_lider(), que comprueba que el agente
     * y controlador estén activos y pertenezcan a la misma plaza.
     */
    @Transactional
    public TrabajadorResponse actualizarAdministracion(
            Long trabajadorId,
            TrabajadorAdminUpdateRequest request
    ) {

        /*
         * ------------------------------------------------------------
         * 1. Buscar trabajador
         * ------------------------------------------------------------
         */

        Trabajador trabajador = trabajadorRepository.findById(trabajadorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Trabajador no encontrado")
                );

        /*
         * ------------------------------------------------------------
         * 2. Buscar nueva plaza
         * ------------------------------------------------------------
         */

        Plaza nuevaPlaza = plazaRepository.findById(request.plazaId())
                .filter(plaza -> Boolean.TRUE.equals(plaza.getActivo()))
                .orElseThrow(() ->
                        new ResourceNotFoundException("Plaza activa no encontrada")
                );

        Long plazaAnteriorId =
                trabajador.getPlaza() != null
                        ? trabajador.getPlaza().getId()
                        : null;

        boolean cambioPlaza =
                !Objects.equals(plazaAnteriorId, nuevaPlaza.getId());

        boolean quedaraInactivo =
                !Boolean.TRUE.equals(request.activo());

        /*
         * ------------------------------------------------------------
         * 3. CERRAR RELACIONES DE LÍDER
         * ------------------------------------------------------------
         *
         * Esto tiene que ejecutarse ANTES de modificar trabajador.plaza
         * o trabajador.activo.
         *
         * De lo contrario el trigger PostgreSQL:
         *
         * validar_agente_controlador_lider()
         *
         * rechaza la operación.
         * ------------------------------------------------------------
         */

        if (cambioPlaza || quedaraInactivo) {

            /*
             * Si el trabajador es AGENTE y actualmente tiene
             * un controlador líder.
             */
            liderRepository
                    .findByAgenteIdAndActivoTrue(trabajadorId)
                    .ifPresent(relacion -> {

                        relacion.setActivo(false);
                        relacion.setFechaFin(LocalDate.now());

                        liderRepository.save(relacion);
                    });

            /*
             * Si el trabajador es CONTROLADOR y tiene agentes
             * actualmente asignados.
             */
            var relacionesComoControlador =
                    liderRepository.findByControladorIdAndActivoTrue(trabajadorId);

            for (var relacion : relacionesComoControlador) {

                relacion.setActivo(false);
                relacion.setFechaFin(LocalDate.now());
            }

            if (!relacionesComoControlador.isEmpty()) {
                liderRepository.saveAll(relacionesComoControlador);
            }

            /*
             * MUY IMPORTANTE:
             *
             * Ejecutamos los UPDATE de las relaciones inmediatamente,
             * mientras el trabajador todavía tiene:
             *
             * - la plaza anterior
             * - su estado activo anterior
             *
             * Así el trigger de PostgreSQL permite cerrar las relaciones.
             */
            liderRepository.flush();
        }

        /*
         * ------------------------------------------------------------
         * 4. ACTUALIZAR TRABAJADOR
         * ------------------------------------------------------------
         */

        trabajador.setPlaza(nuevaPlaza);
        trabajador.setActivo(request.activo());

        /*
         * saveAndFlush garantiza que el cambio se ejecute aquí
         * y no quede pendiente hasta finalizar la transacción.
         */
        trabajadorRepository.saveAndFlush(trabajador);

        /*
         * ------------------------------------------------------------
         * 5. REINICIAR SECUENCIA SI CAMBIÓ DE PLAZA
         * ------------------------------------------------------------
         *
         * No eliminamos el registro.
         *
         * Simplemente:
         *
         * plaza = nueva plaza
         * grupo = null
         * orden = null
         *
         * De esta manera aparecerá como "Sin secuencia" en el módulo
         * de programación y el supervisor podrá reorganizarlo.
         * ------------------------------------------------------------
         */

        if (cambioPlaza) {

            secuenciaRepository
                    .findByAgenteId(trabajadorId)
                    .ifPresent(secuencia -> {

                        secuencia.setPlaza(nuevaPlaza);
                        secuencia.setGrupo(null);
                        secuencia.setOrden(null);

                        secuenciaRepository.save(secuencia);
                    });
        }

        /*
         * ------------------------------------------------------------
         * 6. RESPUESTA
         * ------------------------------------------------------------
         */

        return TrabajadorResponse.from(trabajador);
    }
}