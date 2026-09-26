package com.sigo.incidencia.infrastructure.storage;

import com.sigo.incidencia.application.port.out.IncidenciaStoragePort;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.storage.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CloudinaryIncidenciaStorageAdapter implements IncidenciaStoragePort {

    private final CloudinaryService cloudinaryService;

    @Override
    public UploadResult subir(
            byte[] contenido,
            String contentType
    ) {
        try {
            Map<String, Object> resultado =
                    cloudinaryService.subirImagen(
                            contenido,
                            contentType,
                            "sigo/incidencias"
                    );

            return new UploadResult(
                    String.valueOf(resultado.get("secure_url")),
                    resultado.get("public_id") == null
                            ? null
                            : String.valueOf(resultado.get("public_id"))
            );
        } catch (Exception e) {
            throw new BusinessException(
                    "No se pudo subir la evidencia de la incidencia"
            );
        }
    }

    @Override
    public void eliminar(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        try {
            cloudinaryService.eliminarImagen(publicId);
        } catch (Exception e) {
            log.warn(
                    "No se pudo eliminar una evidencia antigua de Cloudinary. publicId={}",
                    publicId,
                    e
            );
        }
    }
}
