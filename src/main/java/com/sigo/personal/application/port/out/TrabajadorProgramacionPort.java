package com.sigo.personal.application.port.out;

public interface TrabajadorProgramacionPort {

    void cerrarRelacionesAntesDeCambio(
            Long trabajadorId,
            boolean cambioPlaza,
            boolean quedaraInactivo
    );

    void sincronizarSecuenciaDespuesDeCambio(
            Long trabajadorId,
            boolean cambioPlaza,
            Long nuevaPlazaId
    );
}
