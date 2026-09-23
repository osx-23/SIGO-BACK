package com.sigo.relevo.application.port.in;

public interface RelevoAccesoUseCase {

    GestionarRelevoUseCase.Command prepararRegistro(
            GestionarRelevoUseCase.Command command
    );

    void exigirPuedeActualizar();

    RelevoHistorialUseCase.Usuario usuarioActual();
}
