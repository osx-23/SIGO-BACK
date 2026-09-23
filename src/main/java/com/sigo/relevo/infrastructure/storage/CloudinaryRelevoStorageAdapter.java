package com.sigo.relevo.infrastructure.storage;

import com.sigo.relevo.application.port.in.GestionarEvidenciaRelevoUseCase;
import com.sigo.relevo.application.port.out.RelevoStoragePort;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.storage.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CloudinaryRelevoStorageAdapter
        implements RelevoStoragePort {

    private final CloudinaryService cloudinaryService;

    @Override
    public ArchivoSubido subir(
            GestionarEvidenciaRelevoUseCase.ArchivoEntrada archivo,
            String folder
    ) throws IOException {
        Map<String, Object> resultado =
                cloudinaryService.subirImagen(
                        archivo.contenido(),
                        archivo.contentType(),
                        folder
                );

        Object url = resultado.get("secure_url");
        Object publicId = resultado.get("public_id");

        return new ArchivoSubido(
                url == null ? null : url.toString(),
                publicId == null ? null : publicId.toString()
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
