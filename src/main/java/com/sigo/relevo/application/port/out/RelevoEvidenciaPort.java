package com.sigo.relevo.application.port.out;

import com.sigo.relevo.application.port.in.GestionarEvidenciaRelevoUseCase;

public interface RelevoEvidenciaPort {

    boolean existeChecklist(Long checklistId);

    boolean existeRelevoVia(Long relevoViaId);

    GestionarEvidenciaRelevoUseCase.Evidencia guardarChecklist(
            Long checklistId,
            String urlArchivo,
            String publicId
    );

    GestionarEvidenciaRelevoUseCase.Evidencia guardarVia(
            Long relevoViaId,
            String urlArchivo,
            String publicId
    );

    EvidenciaAlmacenada requireChecklist(
            Long checklistId,
            Long evidenciaId
    );

    EvidenciaAlmacenada requireVia(
            Long relevoViaId,
            Long evidenciaId
    );

    void eliminarChecklist(Long evidenciaId);

    void eliminarVia(Long evidenciaId);

    record EvidenciaAlmacenada(
            Long id,
            String urlArchivo,
            String publicId,
            String tipo
    ) {
    }
}
