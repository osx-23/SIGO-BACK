package com.sigo.relevo.application.service;

import com.sigo.relevo.application.port.in.GestionarEvidenciaRelevoUseCase;
import com.sigo.relevo.application.port.out.RelevoEvidenciaPort;
import com.sigo.relevo.application.port.out.RelevoStoragePort;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class GestionarEvidenciaRelevoService
        implements GestionarEvidenciaRelevoUseCase {

    private final RelevoEvidenciaPort evidenciaPort;
    private final RelevoStoragePort storagePort;

    @Override
    @Transactional
    public Evidencia guardarChecklist(
            Long checklistId,
            ArchivoEntrada archivo
    ) throws IOException {
        if (!evidenciaPort.existeChecklist(checklistId)) {
            throw new ResourceNotFoundException(
                    "Elemento de checklist no encontrado"
            );
        }

        validarArchivo(archivo);

        RelevoStoragePort.ArchivoSubido subida =
                storagePort.subir(
                        archivo,
                        "sigo/relevos/checklist"
                );

        validarSubida(subida);

        return evidenciaPort.guardarChecklist(
                checklistId,
                subida.url(),
                subida.publicId()
        );
    }

    @Override
    @Transactional
    public void eliminarChecklist(
            Long checklistId,
            Long evidenciaId
    ) throws IOException {
        RelevoEvidenciaPort.EvidenciaAlmacenada evidencia =
                evidenciaPort.requireChecklist(
                        checklistId,
                        evidenciaId
                );

        eliminarStorage(evidencia.publicId());
        evidenciaPort.eliminarChecklist(evidenciaId);
    }

    @Override
    @Transactional
    public Evidencia guardarVia(
            Long relevoViaId,
            ArchivoEntrada archivo
    ) throws IOException {
        if (!evidenciaPort.existeRelevoVia(relevoViaId)) {
            throw new ResourceNotFoundException(
                    "Reporte de vía no encontrado"
            );
        }

        validarArchivo(archivo);

        RelevoStoragePort.ArchivoSubido subida =
                storagePort.subir(
                        archivo,
                        "sigo/relevos/vias"
                );

        validarSubida(subida);

        return evidenciaPort.guardarVia(
                relevoViaId,
                subida.url(),
                subida.publicId()
        );
    }

    @Override
    @Transactional
    public void eliminarVia(
            Long relevoViaId,
            Long evidenciaId
    ) throws IOException {
        RelevoEvidenciaPort.EvidenciaAlmacenada evidencia =
                evidenciaPort.requireVia(
                        relevoViaId,
                        evidenciaId
                );

        eliminarStorage(evidencia.publicId());
        evidenciaPort.eliminarVia(evidenciaId);
    }

    private void validarArchivo(ArchivoEntrada archivo) {
        if (archivo == null || archivo.vacio()) {
            throw new BusinessException(
                    "Debe seleccionar una imagen"
            );
        }
    }

    private void validarSubida(
            RelevoStoragePort.ArchivoSubido subida
    ) {
        if (subida.url() == null || subida.url().isBlank()) {
            throw new BusinessException(
                    "El almacenamiento no devolvió la URL de la imagen"
            );
        }

        if (subida.publicId() == null
                || subida.publicId().isBlank()) {
            throw new BusinessException(
                    "El almacenamiento no devolvió el identificador de la imagen"
            );
        }
    }

    private void eliminarStorage(String publicId)
            throws IOException {
        if (publicId != null && !publicId.isBlank()) {
            storagePort.eliminar(publicId);
        }
    }
}
