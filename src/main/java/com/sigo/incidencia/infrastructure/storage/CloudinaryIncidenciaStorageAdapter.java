package com.sigo.incidencia.infrastructure.storage;

import com.sigo.incidencia.application.port.out.IncidenciaStoragePort;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.storage.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
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
            throw new BusinessException("No se pudo subir la evidencia de la incidencia");
        }
    }
}
