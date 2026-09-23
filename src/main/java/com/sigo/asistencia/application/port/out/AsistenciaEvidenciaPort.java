package com.sigo.asistencia.application.port.out;

import com.sigo.asistencia.application.port.in.GestionarEvidenciaAsistenciaUseCase;

public interface AsistenciaEvidenciaPort {

    boolean existeAsistencia(Long asistenciaId);

    GestionarEvidenciaAsistenciaUseCase.Evidencia guardar(
            Long asistenciaId,
            String urlArchivo,
            String publicId,
            String tipo
    );

    EvidenciaAlmacenada require(
            Long asistenciaId,
            Long evidenciaId
    );

    void eliminar(Long evidenciaId);

    record EvidenciaAlmacenada(
            Long id,
            String urlArchivo,
            String publicId,
            String tipo
    ) {
    }
}
