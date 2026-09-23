package com.sigo.asistencia.application.port.in;

import java.io.IOException;

public interface GestionarEvidenciaAsistenciaUseCase {

    Evidencia guardar(
            Long asistenciaId,
            ArchivoEntrada archivo,
            String tipo
    ) throws IOException;

    void eliminar(
            Long asistenciaId,
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
            String tipo
    ) {
    }
}
