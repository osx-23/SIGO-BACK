package com.sigo.asistencia.application.service;

import com.sigo.asistencia.application.port.in.GestionarEvidenciaAsistenciaUseCase;
import com.sigo.asistencia.application.port.out.AsistenciaEvidenciaPort;
import com.sigo.asistencia.application.port.out.AsistenciaStoragePort;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GestionarEvidenciaAsistenciaService
        implements GestionarEvidenciaAsistenciaUseCase {

    private static final Set<String> TIPOS_PERMITIDOS =
            Set.of(
                    "CALENTAMIENTO",
                    "INICIO_TURNO",
                    "TAPONES_AUDITIVOS"
            );

    private final AsistenciaEvidenciaPort evidenciaPort;
    private final AsistenciaStoragePort storagePort;

    @Override
    @Transactional
    public Evidencia guardar(
            Long asistenciaId,
            MultipartFile archivo,
            String tipo
    ) throws IOException {
        if (!evidenciaPort.existeAsistencia(asistenciaId)) {
            throw new ResourceNotFoundException(
                    "Asistencia no encontrada"
            );
        }

        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException(
                    "Debe seleccionar una imagen"
            );
        }

        String tipoNormalizado =
                tipo == null
                        ? ""
                        : tipo.trim().toUpperCase(Locale.ROOT);

        if (!TIPOS_PERMITIDOS.contains(tipoNormalizado)) {
            throw new BusinessException(
                    "Tipo de evidencia no válido: " + tipo
            );
        }

        AsistenciaStoragePort.ArchivoSubido subida =
                storagePort.subir(
                        archivo,
                        "sigo/asistencia"
                );

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

        return evidenciaPort.guardar(
                asistenciaId,
                subida.url(),
                subida.publicId(),
                tipoNormalizado
        );
    }

    @Override
    @Transactional
    public void eliminar(
            Long asistenciaId,
            Long evidenciaId
    ) throws IOException {
        AsistenciaEvidenciaPort.EvidenciaAlmacenada evidencia =
                evidenciaPort.require(
                        asistenciaId,
                        evidenciaId
                );

        if (evidencia.publicId() != null
                && !evidencia.publicId().isBlank()) {
            storagePort.eliminar(evidencia.publicId());
        }

        evidenciaPort.eliminar(evidenciaId);
    }
}
