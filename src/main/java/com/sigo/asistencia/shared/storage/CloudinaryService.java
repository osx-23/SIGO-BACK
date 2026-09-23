package com.sigo.asistencia.shared.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public Map<String, Object> subirImagen(MultipartFile archivo) throws IOException {
        return subirImagen(archivo, "sigo/asistencia");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> subirImagen(MultipartFile archivo, String folder) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar una imagen");
        }

        if (archivo.getContentType() == null || !archivo.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen");
        }

        byte[] contenido = archivo.getBytes();
        if (!esImagenReal(contenido)) {
            throw new IllegalArgumentException("El contenido del archivo no corresponde a una imagen permitida");
        }

        String carpeta = folder == null || folder.isBlank() ? "sigo" : folder.trim();

        try {
            Map<?, ?> resultado = cloudinary.uploader().upload(
                    contenido,
                    ObjectUtils.asMap(
                            "folder", carpeta,
                            "resource_type", "image"
                    )
            );
            return (Map<String, Object>) resultado;
        } catch (Exception e) {
            log.error("Error al subir imagen a Cloudinary. folder={}", carpeta, e);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> eliminarImagen(String publicId) throws IOException {
        if (publicId == null || publicId.isBlank()) {
            throw new IllegalArgumentException("El public_id de la imagen es obligatorio");
        }

        try {
            return (Map<String, Object>) cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap(
                            "resource_type", "image",
                            "invalidate", true
                    )
            );
        } catch (Exception e) {
            log.error("Error al eliminar imagen de Cloudinary. publicId={}", publicId, e);
            throw e;
        }
    }

    private boolean esImagenReal(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return false;

        boolean jpeg = (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;

        boolean png = (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;

        boolean gif = bytes[0] == 'G'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == '8'
                && (bytes[4] == '7' || bytes[4] == '9')
                && bytes[5] == 'a';

        boolean webp = bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P';

        return jpeg || png || gif || webp;
    }
}
