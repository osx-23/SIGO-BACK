package com.sigo.incidencia.application.port.out;

public interface IncidenciaStoragePort {

    UploadResult subir(
            byte[] contenido,
            String contentType
    );

    void eliminar(String publicId);

    record UploadResult(
            String urlArchivo,
            String publicId
    ) {
    }
}
