package com.sigo.asistencia.application.service;

import com.sigo.asistencia.application.port.in.GestionarEvidenciaAsistenciaUseCase;
import com.sigo.asistencia.application.port.out.AsistenciaEvidenciaPort;
import com.sigo.asistencia.application.port.out.AsistenciaStoragePort;
import com.sigo.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GestionarEvidenciaAsistenciaServiceTest {

    @Test
    void normalizaTipoYGuardaResultadoDeStorage() throws IOException {
        StubEvidenciaPort evidenciaPort =
                new StubEvidenciaPort();

        GestionarEvidenciaAsistenciaService service =
                new GestionarEvidenciaAsistenciaService(
                        evidenciaPort,
                        new StubStoragePort()
                );

        var evidencia = service.guardar(
                1L,
                new GestionarEvidenciaAsistenciaUseCase.ArchivoEntrada(
                        "foto.png",
                        "image/png",
                        new byte[]{1, 2, 3}
                ),
                " inicio_turno "
        );

        assertEquals("INICIO_TURNO", evidencia.tipo());
        assertEquals("https://storage/foto.png", evidencia.urlArchivo());
        assertEquals("public-1", evidenciaPort.publicId);
    }

    @Test
    void rechazaTipoNoPermitido() {
        GestionarEvidenciaAsistenciaService service =
                new GestionarEvidenciaAsistenciaService(
                        new StubEvidenciaPort(),
                        new StubStoragePort()
                );

        assertThrows(
                BusinessException.class,
                () -> service.guardar(
                        1L,
                        new GestionarEvidenciaAsistenciaUseCase.ArchivoEntrada(
                                "foto.png",
                                "image/png",
                                new byte[]{1}
                        ),
                        "OTRO"
                )
        );
    }

    private static class StubEvidenciaPort
            implements AsistenciaEvidenciaPort {

        private String publicId;

        @Override
        public boolean existeAsistencia(Long asistenciaId) {
            return true;
        }

        @Override
        public GestionarEvidenciaAsistenciaUseCase.Evidencia guardar(
                Long asistenciaId,
                String urlArchivo,
                String publicId,
                String tipo
        ) {
            this.publicId = publicId;

            return new GestionarEvidenciaAsistenciaUseCase.Evidencia(
                    5L,
                    urlArchivo,
                    tipo
            );
        }

        @Override
        public EvidenciaAlmacenada require(
                Long asistenciaId,
                Long evidenciaId
        ) {
            return new EvidenciaAlmacenada(
                    evidenciaId,
                    "https://storage/foto.png",
                    "public-1",
                    "INICIO_TURNO"
            );
        }

        @Override
        public void eliminar(Long evidenciaId) {
        }
    }

    private static class StubStoragePort
            implements AsistenciaStoragePort {

        @Override
        public ArchivoSubido subir(
                GestionarEvidenciaAsistenciaUseCase.ArchivoEntrada archivo,
                String folder
        ) {
            return new ArchivoSubido(
                    "https://storage/foto.png",
                    "public-1"
            );
        }

        @Override
        public void eliminar(String publicId) {
        }
    }
}
