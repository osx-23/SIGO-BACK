package com.sigo.inventario.application.port.out;

import com.sigo.inventario.application.port.in.IniciarInventarioUseCase;

import java.util.Optional;

public interface IniciarInventarioPort {

    Optional<Long> inventarioEnProcesoId(Long responsableId);

    IniciarInventarioUseCase.Resumen crear(
            IniciarInventarioUseCase.Usuario usuario
    );
}
