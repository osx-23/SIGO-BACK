package com.sigo.personal.application.service;

import com.sigo.personal.application.port.in.TrabajadorUseCase;
import com.sigo.personal.application.port.out.TrabajadorGestionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrabajadorService implements TrabajadorUseCase {

    private final TrabajadorGestionPort gestionPort;

    @Override
    @Transactional(readOnly = true)
    public List<TrabajadorData> listarAgentesPorPlaza(
            Long plazaId
    ) {
        return gestionPort.listarAgentesPorPlaza(plazaId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrabajadorData> listarControladoresPorPlaza(
            Long plazaId
    ) {
        return gestionPort.listarControladoresPorPlaza(plazaId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrabajadorData> listarAdministracion(
            Long plazaId
    ) {
        return gestionPort.listarAdministracion(plazaId);
    }

    @Override
    @Transactional
    public TrabajadorData actualizarAdministracion(
            Long trabajadorId,
            Long plazaId,
            Boolean activo
    ) {
        if (plazaId == null || activo == null) {
            throw new IllegalArgumentException(
                    "Plaza y estado son obligatorios"
            );
        }

        return gestionPort.actualizarAdministracion(
                trabajadorId,
                plazaId,
                activo
        );
    }
}
