package com.sigo.relevo.application.service;

import com.sigo.relevo.application.port.in.ConfigurarViasAviUseCase;
import com.sigo.relevo.application.port.out.ViaConsultaPort;
import com.sigo.security.application.port.in.UsuarioActualUseCase;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ForbiddenException;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ViaAviConfigService
        implements ConfigurarViasAviUseCase {

    private final ViaConsultaPort viaConsultaPort;
    private final UsuarioActualUseCase usuarioActualUseCase;

    @Override
    @Transactional(readOnly = true)
    public List<ViaConfig> listar(Long plazaId) {
        requireSupervisor();
        validarPlaza(plazaId);
        return viaConsultaPort.listarActivasParaConfigAvi(plazaId);
    }

    @Override
    @Transactional
    public void actualizar(
            Long plazaId,
            Long viaId,
            boolean visible
    ) {
        requireSupervisor();
        validarPlaza(plazaId);

        List<ViaConfig> vias =
                viaConsultaPort.listarActivasParaConfigAvi(plazaId);

        ViaConfig objetivo =
                vias.stream()
                        .filter(via -> via.id().equals(viaId))
                        .findFirst()
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Vía no encontrada en la plaza"
                                )
                        );

        if (
                !visible &&
                Boolean.TRUE.equals(objetivo.aviVisible())
        ) {
            long visibles =
                    vias.stream()
                            .filter(via ->
                                    Boolean.TRUE.equals(
                                            via.aviVisible()
                                    )
                            )
                            .count();

            if (visibles <= 1) {
                throw new BusinessException(
                        "AVIX debe mantener al menos una vía visible por plaza"
                );
            }
        }

        viaConsultaPort.actualizarVisibilidadAvi(
                plazaId,
                viaId,
                visible
        );
    }

    private void validarPlaza(Long plazaId) {
        if (
                plazaId == null ||
                !viaConsultaPort.existePlaza(plazaId)
        ) {
            throw new ResourceNotFoundException(
                    "Plaza no encontrada"
            );
        }
    }

    private void requireSupervisor() {
        UsuarioActualUseCase.UsuarioActual actual =
                usuarioActualUseCase.requireActual();

        if (
                actual.rol() == null ||
                !"SUPERVISOR".equalsIgnoreCase(
                        actual.rol()
                )
        ) {
            throw new ForbiddenException(
                    "Solo un supervisor puede configurar las vías visibles en AVIX"
            );
        }
    }
}
