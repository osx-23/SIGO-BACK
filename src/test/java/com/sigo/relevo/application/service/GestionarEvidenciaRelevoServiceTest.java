package com.sigo.relevo.application.service;

import com.sigo.relevo.application.port.in.GestionarEvidenciaRelevoUseCase;
import com.sigo.relevo.application.port.out.RelevoEvidenciaPort;
import com.sigo.relevo.application.port.out.RelevoStoragePort;
import com.sigo.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GestionarEvidenciaRelevoServiceTest {

    @Test
    void guardaEvidenciaDeChecklist() throws IOException {
        StubEvidenciaPort evidenciaPort =
                new StubEvidenciaPort();

        GestionarEvidenciaRelevoService service =
                new GestionarEvidenciaRelevoService(
                        evidenciaPort,
                        new StubStoragePort()
                );

        var evidencia = service.guardarChecklist(
                10L,
                new GestionarEvidenciaRelevoUseCase.ArchivoEntrada(
                        "foto.png",
                        "image/png",
                        new byte[]{1, 2, 3}
                )
        );

        assertEquals(
                "https://storage/relevo.png",
                evidencia.urlArchivo()
        );

        assertEquals(
                "public-relevo",
                evidenciaPort.publicId
        );
    }

    @Test
    void rechazaArchivoVacio() {
        GestionarEvidenciaRelevoService service =
                new GestionarEvidenciaRelevoService(
                        new StubEvidenciaPort(),
                        new StubStoragePort()
                );

        assertThrows(
                BusinessException.class,
                () -> service.guardarVia(
                        20L,
                        new GestionarEvidenciaRelevoUseCase.ArchivoEntrada(
                                "foto.png",
                                "image/png",
                                new byte[0]
                        )
                )
        );
    }

    private static class StubEvidenciaPort
            implements RelevoEvidenciaPort {

        private String publicId;

        @Override
        public boolean existeChecklist(Long checklistId) {
            return true;
        }

        @Override
        public boolean existeRelevoVia(Long relevoViaId) {
            return true;
        }

        @Override
        public GestionarEvidenciaRelevoUseCase.Evidencia guardarChecklist(
                Long checklistId,
                String urlArchivo,
                String publicId
        ) {
            this.publicId = publicId;

            return evidencia(
                    1L,
                    urlArchivo,
                    publicId
            );
        }

        @Override
        public GestionarEvidenciaRelevoUseCase.Evidencia guardarVia(
                Long relevoViaId,
                String urlArchivo,
                String publicId
        ) {
            this.publicId = publicId;

            return evidencia(
                    2L,
                    urlArchivo,
                    publicId
            );
        }

        @Override
        public EvidenciaAlmacenada requireChecklist(
                Long checklistId,
                Long evidenciaId
        ) {
            return new EvidenciaAlmacenada(
                    evidenciaId,
                    "https://storage/relevo.png",
                    "public-relevo",
                    "foto"
            );
        }

        @Override
        public EvidenciaAlmacenada requireVia(
                Long relevoViaId,
                Long evidenciaId
        ) {
            return new EvidenciaAlmacenada(
                    evidenciaId,
                    "https://storage/relevo.png",
                    "public-relevo",
                    "foto"
            );
        }

        @Override
        public void eliminarChecklist(Long evidenciaId) {
        }

        @Override
        public void eliminarVia(Long evidenciaId) {
        }

        private GestionarEvidenciaRelevoUseCase.Evidencia evidencia(
                Long id,
                String url,
                String publicId
        ) {
            return new GestionarEvidenciaRelevoUseCase.Evidencia(
                    id,
                    url,
                    publicId,
                    "foto",
                    OffsetDateTime.now()
            );
        }
    }

    private static class StubStoragePort
            implements RelevoStoragePort {

        @Override
        public ArchivoSubido subir(
                GestionarEvidenciaRelevoUseCase.ArchivoEntrada archivo,
                String folder
        ) {
            return new ArchivoSubido(
                    "https://storage/relevo.png",
                    "public-relevo"
            );
        }

        @Override
        public void eliminar(String publicId) {
        }
    }
}
