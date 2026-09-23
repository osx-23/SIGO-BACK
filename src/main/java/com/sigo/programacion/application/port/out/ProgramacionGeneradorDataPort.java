package com.sigo.programacion.application.port.out;

import com.sigo.programacion.domain.generador.GeneradorExcepcion;
import com.sigo.programacion.domain.generador.GeneradorPlaza;
import com.sigo.programacion.domain.generador.GeneradorSecuencia;
import com.sigo.programacion.domain.generador.GeneradorTrabajador;
import com.sigo.programacion.domain.generador.GeneradorTurno;

import java.time.LocalDate;
import java.util.List;

public interface ProgramacionGeneradorDataPort {

    GeneradorPlaza requirePlazaActiva(Long plazaId);

    List<GeneradorTrabajador> agentesPorPlaza(Long plazaId);

    List<GeneradorSecuencia> secuenciasPorPlaza(Long plazaId);

    List<GeneradorExcepcion> excepcionesPorPlaza(Long plazaId);

    List<GeneradorTurno> historial(
            Long plazaId,
            LocalDate desde,
            LocalDate hasta
    );
}
