package com.sigo.asistencia.application.port.out;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface AsistenciaStoragePort {

    ArchivoSubido subir(
            MultipartFile archivo,
            String folder
    ) throws IOException;

    void eliminar(String publicId) throws IOException;

    record ArchivoSubido(
            String url,
            String publicId
    ) {
    }
}
