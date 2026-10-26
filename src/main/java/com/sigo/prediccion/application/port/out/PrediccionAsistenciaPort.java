package com.sigo.prediccion.application.port.out;

import com.sigo.prediccion.domain.PrediccionAsistenciaParametros;
import com.sigo.prediccion.domain.PrediccionAsistenciaResultado;

public interface PrediccionAsistenciaPort {
    PrediccionAsistenciaResultado predecir(PrediccionAsistenciaParametros parametros);
}
