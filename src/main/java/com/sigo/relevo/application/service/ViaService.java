package com.sigo.relevo.application.service;

import com.sigo.relevo.application.port.in.ListarViasUseCase;
import com.sigo.relevo.application.port.out.ViaConsultaPort;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ViaService implements ListarViasUseCase {

    private final ViaConsultaPort viaConsultaPort;

    @Override
    public List<Via> listarPorPlaza(Long plazaId) {
        validarPlaza(plazaId);
        return viaConsultaPort.listarActivas(plazaId);
    }

    @Override
    public List<Via> listarVisiblesAvi(Long plazaId) {
        validarPlaza(plazaId);
        return viaConsultaPort.listarActivasVisiblesAvi(plazaId);
    }

    private void validarPlaza(Long plazaId) {
        if (!viaConsultaPort.existePlaza(plazaId)) {
            throw new ResourceNotFoundException(
                    "Plaza no encontrada"
            );
        }
    }
}
