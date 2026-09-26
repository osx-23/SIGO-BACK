package com.sigo.personal.application.port.out;

public interface TrabajadorProgramacionPort {

    void cerrarRelacionesAntesDeCambio(
            Long trabajadorId,
            boolean cambioConfiguracion,
            boolean quedaraInactivo
    );

    void sincronizarSecuenciaDespuesDeCambio(
            Long trabajadorId,
            boolean cambioConfiguracion,
            Long nuevaPlazaId,
            boolean esOperador
    );
}
