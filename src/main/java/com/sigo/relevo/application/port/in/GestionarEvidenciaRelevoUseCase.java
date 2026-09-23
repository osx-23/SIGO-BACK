package com.sigo.relevo.application.port.in;

import java.io.IOException;
import java.time.OffsetDateTime;

public interface GestionarEvidenciaRelevoUseCase {

    Evidencia guardarChecklist(
            Long checklistId,
            ArchivoEntrada archivo
    ) throws IOException;

    void eliminarChecklist(
            Long checklistId,
            Long evidenciaId
    ) throws IOException;

    Evidencia guardarVia(
            Long relevoViaId,
            ArchivoEntrada archivo
    ) throws IOException;

    void eliminarVia(
            Long relevoViaId,
            Long evidenciaId
    ) throws IOException;

    record ArchivoEntrada(
            String nombre,
            String contentType,
            byte[] contenido
    ) {
        public boolean vacio() {
            return contenido == null || contenido.length == 0;
        }
    }

    record Evidencia(
            Long id,
            String urlArchivo,
            String publicId,
            String tipo,
            OffsetDateTime createdAt
    ) {
    }
}
