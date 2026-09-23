package com.sigo.relevo.application.port.out;

import com.sigo.relevo.application.port.in.GestionarEvidenciaRelevoUseCase;

import java.io.IOException;

public interface RelevoStoragePort {

    ArchivoSubido subir(
            GestionarEvidenciaRelevoUseCase.ArchivoEntrada archivo,
            String folder
    ) throws IOException;

    void eliminar(String publicId) throws IOException;

    record ArchivoSubido(
            String url,
            String publicId
    ) {
    }
}
