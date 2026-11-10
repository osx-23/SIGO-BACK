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
                        new StubStoragePort(),
                        org.mockito.Mockito.mock(com.sigo.relevo.application.port.in.RelevoHistorialUseCase.class),
                        org.mockito.Mockito.mock(com.sigo.relevo.application.port.in.RelevoAccesoUseCase.class)
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
                        new StubStoragePort(),
                        org.mockito.Mockito.mock(com.sigo.relevo.application.port.in.RelevoHistorialUseCase.class),
                        org.mockito.Mockito.mock(com.sigo.relevo.application.port.in.RelevoAccesoUseCase.class)
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

    @Test
    void rechazaModificacionesDeOtroRelevoAntesDeAccederAlAlmacenamiento() throws IOException {
        var port = org.mockito.Mockito.mock(RelevoEvidenciaPort.class);
        var storage = org.mockito.Mockito.mock(RelevoStoragePort.class);
        var historial = org.mockito.Mockito.mock(com.sigo.relevo.application.port.in.RelevoHistorialUseCase.class);
        var acceso = org.mockito.Mockito.mock(com.sigo.relevo.application.port.in.RelevoAccesoUseCase.class);
        var usuario = new com.sigo.relevo.application.port.in.RelevoHistorialUseCase.Usuario("OPERADOR", 1L);
        org.mockito.Mockito.when(acceso.usuarioActual()).thenReturn(usuario);
        org.mockito.Mockito.when(port.relevoDeChecklist(10L)).thenReturn(100L);
        org.mockito.Mockito.when(port.relevoDeVia(20L)).thenReturn(200L);
        org.mockito.Mockito.doThrow(new com.sigo.shared.exception.ForbiddenException("Sin acceso"))
                .when(historial).obtenerPara(org.mockito.ArgumentMatchers.eq(usuario), org.mockito.ArgumentMatchers.anyLong());
        var service = new GestionarEvidenciaRelevoService(port, storage, historial, acceso);
        var archivo = new GestionarEvidenciaRelevoUseCase.ArchivoEntrada("a.png", "image/png", new byte[]{1});
        assertThrows(com.sigo.shared.exception.ForbiddenException.class, () -> service.guardarChecklist(10L, archivo));
        assertThrows(com.sigo.shared.exception.ForbiddenException.class, () -> service.eliminarChecklist(10L, 11L));
        assertThrows(com.sigo.shared.exception.ForbiddenException.class, () -> service.guardarVia(20L, archivo));
        assertThrows(com.sigo.shared.exception.ForbiddenException.class, () -> service.eliminarVia(20L, 21L));
        org.mockito.Mockito.verifyNoInteractions(storage);
    }

    private static class StubEvidenciaPort
            implements RelevoEvidenciaPort {

        private String publicId;
        @Override public Long relevoDeChecklist(Long id) { return 1L; }
        @Override public Long relevoDeVia(Long id) { return 1L; }

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
