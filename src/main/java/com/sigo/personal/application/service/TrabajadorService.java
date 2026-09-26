package com.sigo.personal.application.service;

import com.sigo.personal.application.port.in.TrabajadorUseCase;
import com.sigo.personal.application.port.out.TrabajadorGestionPort;
import com.sigo.personal.application.port.out.TrabajadorProgramacionPort;
import com.sigo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrabajadorService implements TrabajadorUseCase {

    private final TrabajadorGestionPort gestionPort;
    private final TrabajadorProgramacionPort programacionPort;

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
    @Transactional(readOnly = true)
    public List<PuestoData> listarPuestosAgente() {
        return gestionPort.listarPuestosAgente();
    }

    @Override
    @Transactional
    public TrabajadorData crearAgente(
            Integer codigo,
            String nombreCompleto,
            Long puestoId,
            Long plazaId,
            String passwordInicial
    ) {
        if (codigo == null || codigo <= 0) {
            throw new BusinessException(
                    "El código del agente es obligatorio"
            );
        }

        String nombre =
                nombreCompleto == null
                        ? ""
                        : nombreCompleto.trim();

        if (nombre.length() < 3) {
            throw new BusinessException(
                    "Ingresa el nombre completo del agente"
            );
        }

        if (puestoId == null || plazaId == null) {
            throw new BusinessException(
                    "Puesto y plaza son obligatorios"
            );
        }

        String password =
                passwordInicial == null
                        ? ""
                        : passwordInicial.trim();

        if (password.length() < 5) {
            throw new BusinessException(
                    "La contraseña inicial debe tener al menos 5 caracteres"
            );
        }

        if (gestionPort.existeCodigo(codigo)) {
            throw new BusinessException(
                    "Ya existe un trabajador con el código " + codigo
            );
        }

        return gestionPort.crearAgente(
                codigo,
                nombre,
                puestoId,
                plazaId,
                password
        );
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

        TrabajadorGestionPort.PreparacionActualizacion preparacion =
                gestionPort.prepararActualizacion(
                        trabajadorId,
                        plazaId,
                        activo
                );

        programacionPort.cerrarRelacionesAntesDeCambio(
                trabajadorId,
                preparacion.cambioPlaza(),
                preparacion.quedaraInactivo()
        );

        TrabajadorData actualizado =
                gestionPort.aplicarActualizacion(
                        trabajadorId,
                        plazaId,
                        activo
                );

        programacionPort.sincronizarSecuenciaDespuesDeCambio(
                trabajadorId,
                preparacion.cambioPlaza(),
                plazaId
        );

        return actualizado;
    }
}
