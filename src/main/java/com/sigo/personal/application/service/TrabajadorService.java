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
    public List<PuestoData> listarPuestosAdministrables() {
        return gestionPort.listarPuestosAdministrables();
    }

    @Override
    @Transactional
    public TrabajadorData crearUsuario(
            Integer codigo,
            String nombreCompleto,
            Long puestoId,
            Long plazaId,
            String passwordInicial
    ) {
        if (codigo == null || codigo <= 0) {
            throw new BusinessException(
                    "El código del trabajador es obligatorio"
            );
        }

        String nombre =
                nombreCompleto == null
                        ? ""
                        : nombreCompleto.trim();

        if (nombre.length() < 3) {
            throw new BusinessException(
                    "Ingresa el nombre completo del trabajador"
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

        return gestionPort.crearUsuario(
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
            Long puestoId,
            Boolean activo
    ) {
        if (plazaId == null || puestoId == null || activo == null) {
            throw new IllegalArgumentException(
                    "Plaza, puesto y estado son obligatorios"
            );
        }

        TrabajadorGestionPort.PreparacionActualizacion preparacion =
                gestionPort.prepararActualizacion(
                        trabajadorId,
                        plazaId,
                        puestoId,
                        activo
                );

        boolean cambioConfiguracion =
                preparacion.cambioPlaza() ||
                preparacion.cambioPuesto();

        programacionPort.cerrarRelacionesAntesDeCambio(
                trabajadorId,
                cambioConfiguracion,
                preparacion.quedaraInactivo()
        );

        TrabajadorData actualizado =
                gestionPort.aplicarActualizacion(
                        trabajadorId,
                        plazaId,
                        puestoId,
                        activo
                );

        programacionPort.sincronizarSecuenciaDespuesDeCambio(
                trabajadorId,
                cambioConfiguracion,
                plazaId,
                preparacion.nuevoEsOperador()
        );

        return actualizado;
    }
}
