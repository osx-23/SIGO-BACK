package com.sigo.asistencia.application.port.out;

import com.sigo.asistencia.application.port.in.GestionarEvidenciaAsistenciaUseCase;

import java.io.IOException;

public interface AsistenciaStoragePort {

    ArchivoSubido subir(
            GestionarEvidenciaAsistenciaUseCase.ArchivoEntrada archivo,
            String folder
    ) throws IOException;

    void eliminar(String publicId) throws IOException;

    record ArchivoSubido(
            String url,
            String publicId
    ) {
    }
}
