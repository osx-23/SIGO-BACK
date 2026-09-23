package com.sigo.personal.infrastructure.persistence.adapter;

import com.sigo.personal.application.port.in.TrabajadorUseCase;
import com.sigo.personal.application.port.out.TrabajadorGestionPort;
import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Puesto;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.programacion.infrastructure.persistence.repository.AgenteControladorLiderRepository;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionSecuenciaAgenteRepository;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class TrabajadorGestionJpaAdapter
        implements TrabajadorGestionPort {

    private final TrabajadorRepository trabajadorRepository;
    private final PlazaRepository plazaRepository;
    private final ProgramacionSecuenciaAgenteRepository secuenciaRepository;
    private final AgenteControladorLiderRepository liderRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TrabajadorUseCase.TrabajadorData> listarAgentesPorPlaza(
            Long plazaId
    ) {
        return trabajadorRepository
                .findAgentesByPlaza(plazaId)
                .stream()
                .map(this::toData)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrabajadorUseCase.TrabajadorData> listarControladoresPorPlaza(
            Long plazaId
    ) {
        return trabajadorRepository
                .findControladoresByPlaza(plazaId)
                .stream()
                .map(this::toData)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrabajadorUseCase.TrabajadorData> listarAdministracion(
            Long plazaId
    ) {
        List<Trabajador> trabajadores =
                plazaId == null
                        ? trabajadorRepository.findAllAdminOptimizado()
                        : trabajadorRepository.findAllAdminByPlazaOptimizado(
                                plazaId
                        );

        return trabajadores
                .stream()
                .map(this::toData)
                .toList();
    }

    @Override
    @Transactional
    public TrabajadorUseCase.TrabajadorData actualizarAdministracion(
            Long trabajadorId,
            Long plazaId,
            Boolean activo
    ) {
        Trabajador trabajador = trabajadorRepository
                .findById(trabajadorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Trabajador no encontrado"
                        )
                );

        Plaza nuevaPlaza = plazaRepository
                .findById(plazaId)
                .filter(plaza ->
                        Boolean.TRUE.equals(plaza.getActivo())
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Plaza activa no encontrada"
                        )
                );

        Long plazaAnteriorId =
                trabajador.getPlaza() == null
                        ? null
                        : trabajador.getPlaza().getId();

        boolean cambioPlaza =
                !Objects.equals(
                        plazaAnteriorId,
                        nuevaPlaza.getId()
                );

        boolean quedaraInactivo =
                !Boolean.TRUE.equals(activo);

        /*
         * Las relaciones se cierran antes de tocar plaza/activo.
         * Esto conserva el orden requerido por los triggers de PostgreSQL.
         */
        if (cambioPlaza || quedaraInactivo) {
            liderRepository
                    .findByAgenteIdAndActivoTrue(trabajadorId)
                    .ifPresent(relacion -> {
                        relacion.setActivo(false);
                        relacion.setFechaFin(LocalDate.now());
                        liderRepository.save(relacion);
                    });

            var relacionesComoControlador =
                    liderRepository
                            .findByControladorIdAndActivoTrue(
                                    trabajadorId
                            );

            for (var relacion : relacionesComoControlador) {
                relacion.setActivo(false);
                relacion.setFechaFin(LocalDate.now());
            }

            if (!relacionesComoControlador.isEmpty()) {
                liderRepository.saveAll(relacionesComoControlador);
            }

            liderRepository.flush();
        }

        trabajador.setPlaza(nuevaPlaza);
        trabajador.setActivo(activo);
        trabajadorRepository.saveAndFlush(trabajador);

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

        return toData(trabajador);
    }

    private TrabajadorUseCase.TrabajadorData toData(
            Trabajador trabajador
    ) {
        return new TrabajadorUseCase.TrabajadorData(
                trabajador.getId(),
                trabajador.getCodigo(),
                trabajador.getNombreCompleto(),
                toPuesto(trabajador.getPuesto()),
                toPlaza(trabajador.getPlaza()),
                trabajador.getRolSistema() == null
                        ? null
                        : trabajador.getRolSistema().name(),
                trabajador.getRequiereCambioPassword(),
                trabajador.getActivo()
        );
    }

    private TrabajadorUseCase.PuestoData toPuesto(
            Puesto puesto
    ) {
        return puesto == null
                ? null
                : new TrabajadorUseCase.PuestoData(
                        puesto.getId(),
                        puesto.getNombre()
                );
    }

    private TrabajadorUseCase.PlazaData toPlaza(
            Plaza plaza
    ) {
        return plaza == null
                ? null
                : new TrabajadorUseCase.PlazaData(
                        plaza.getId(),
                        plaza.getCodigo(),
                        plaza.getDescripcion(),
                        plaza.getActivo()
                );
    }
}
