package com.sigo.asistencia.application.port.in;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface GestionarEvidenciaAsistenciaUseCase {

    Evidencia guardar(
            Long asistenciaId,
            MultipartFile archivo,
            String tipo
    ) throws IOException;

    void eliminar(
            Long asistenciaId,
            Long evidenciaId
    ) throws IOException;

    record Evidencia(
            Long id,
            String urlArchivo,
            String tipo
    ) {
    }
}
