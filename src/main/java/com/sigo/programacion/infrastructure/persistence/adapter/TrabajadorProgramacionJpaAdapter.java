package com.sigo.programacion.infrastructure.persistence.adapter;

import com.sigo.personal.application.port.out.TrabajadorProgramacionPort;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.programacion.infrastructure.persistence.repository.AgenteControladorLiderRepository;
import com.sigo.programacion.infrastructure.persistence.repository.ProgramacionSecuenciaAgenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class TrabajadorProgramacionJpaAdapter
        implements TrabajadorProgramacionPort {

    private final AgenteControladorLiderRepository liderRepository;
    private final ProgramacionSecuenciaAgenteRepository secuenciaRepository;
    private final PlazaRepository plazaRepository;

    @Override
    public void cerrarRelacionesAntesDeCambio(
            Long trabajadorId,
            boolean cambioPlaza,
            boolean quedaraInactivo
    ) {
        if (!cambioPlaza && !quedaraInactivo) {
            return;
        }

        liderRepository
                .findByAgenteIdAndActivoTrue(trabajadorId)
                .ifPresent(relacion -> {
                    relacion.setActivo(false);
                    relacion.setFechaFin(LocalDate.now());
                    liderRepository.save(relacion);
                });

        var relacionesComoControlador =
                liderRepository.findByControladorIdAndActivoTrue(
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

    @Override
    public void sincronizarSecuenciaDespuesDeCambio(
            Long trabajadorId,
            boolean cambioPlaza,
            Long nuevaPlazaId
    ) {
        if (!cambioPlaza) {
            return;
        }

        var nuevaPlaza = plazaRepository
                .findById(nuevaPlazaId)
                .orElseThrow();

        secuenciaRepository
                .findByAgenteId(trabajadorId)
                .ifPresent(secuencia -> {
                    secuencia.setPlaza(nuevaPlaza);
                    secuencia.setGrupo(null);
                    secuencia.setOrden(null);
                    secuenciaRepository.save(secuencia);
                });
    }
}
