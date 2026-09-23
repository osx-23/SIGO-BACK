package com.sigo.asistencia.infrastructure.persistence.adapter;

import com.sigo.asistencia.application.port.in.GestionarEvidenciaAsistenciaUseCase;
import com.sigo.asistencia.application.port.out.AsistenciaEvidenciaPort;
import com.sigo.asistencia.infrastructure.persistence.entity.AsistenciaEvidencia;
import com.sigo.asistencia.infrastructure.persistence.entity.AsistenciaRegistro;
import com.sigo.asistencia.infrastructure.persistence.repository.AsistenciaEvidenciaRepository;
import com.sigo.asistencia.infrastructure.persistence.repository.AsistenciaRepository;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AsistenciaEvidenciaJpaAdapter
        implements AsistenciaEvidenciaPort {

    private final AsistenciaRepository asistenciaRepository;
    private final AsistenciaEvidenciaRepository evidenciaRepository;

    @Override
    public boolean existeAsistencia(Long asistenciaId) {
        return asistenciaRepository.existsById(asistenciaId);
    }

    @Override
    public GestionarEvidenciaAsistenciaUseCase.Evidencia guardar(
            Long asistenciaId,
            String urlArchivo,
            String publicId,
            String tipo
    ) {
        AsistenciaRegistro asistencia = asistenciaRepository
                .findById(asistenciaId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Asistencia no encontrada"
                        )
                );

        AsistenciaEvidencia evidencia =
                new AsistenciaEvidencia();

        evidencia.setAsistencia(asistencia);
        evidencia.setUrlArchivo(urlArchivo);
        evidencia.setPublicId(publicId);
        evidencia.setTipo(tipo);

        AsistenciaEvidencia guardada =
                evidenciaRepository.save(evidencia);

        return new GestionarEvidenciaAsistenciaUseCase.Evidencia(
                guardada.getId(),
                guardada.getUrlArchivo(),
                guardada.getTipo()
        );
    }

    @Override
    public EvidenciaAlmacenada require(
            Long asistenciaId,
            Long evidenciaId
    ) {
        AsistenciaEvidencia evidencia = evidenciaRepository
                .findByIdAndAsistenciaId(
                        evidenciaId,
                        asistenciaId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Evidencia no encontrada"
                        )
                );

        return new EvidenciaAlmacenada(
                evidencia.getId(),
                evidencia.getUrlArchivo(),
                evidencia.getPublicId(),
                evidencia.getTipo()
        );
    }

    @Override
    public void eliminar(Long evidenciaId) {
        evidenciaRepository.deleteById(evidenciaId);
    }
}
