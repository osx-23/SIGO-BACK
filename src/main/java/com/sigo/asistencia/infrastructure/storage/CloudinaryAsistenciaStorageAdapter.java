package com.sigo.asistencia.infrastructure.storage;

import com.sigo.asistencia.application.port.out.AsistenciaStoragePort;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.storage.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CloudinaryAsistenciaStorageAdapter
        implements AsistenciaStoragePort {

    private final CloudinaryService cloudinaryService;

    @Override
    public ArchivoSubido subir(
            GestionarEvidenciaAsistenciaUseCase.ArchivoEntrada archivo,
            String folder
    ) throws IOException {
        Map<String, Object> resultado =
                cloudinaryService.subirImagen(
                        archivo.contenido(),
                        archivo.contentType(),
                        folder
                );

        Object secureUrl = resultado.get("secure_url");
        Object publicId = resultado.get("public_id");

        return new ArchivoSubido(
                secureUrl == null
                        ? null
                        : secureUrl.toString(),
                publicId == null
                        ? null
                        : publicId.toString()
        );
    }

    @Override
    public void eliminar(String publicId) throws IOException {
        Map<String, Object> resultado =
                cloudinaryService.eliminarImagen(publicId);

        Object estado = resultado.get("result");

        if (estado != null
                && !"ok".equalsIgnoreCase(estado.toString())
                && !"not found".equalsIgnoreCase(
                        estado.toString()
                )) {
            throw new BusinessException(
                    "No se pudo eliminar la imagen de Cloudinary"
            );
        }
    }
}
